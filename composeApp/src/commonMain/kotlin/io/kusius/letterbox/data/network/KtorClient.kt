package io.kusius.letterbox.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.kusius.letterbox.data.network.model.ApiModel
import io.kusius.letterbox.data.network.model.TokenInfo
import io.kusius.letterbox.domain.auth.AuthResult
import io.kusius.letterbox.domain.auth.Authenticator
import kotlinx.serialization.json.Json

sealed interface ApiResult<T> {
    class Success<T>(
        data: T,
    ) : ApiResult<T>

    object Unauthorized : ApiResult<Nothing>

    class GenericError(
        e: Throwable,
    ) : ApiResult<Nothing>
}

private const val GOOGLE_APIS = "gmail.googleapis.com"
private const val API_BASE_PATH = "/gmail/v1"
private const val API_BATCH_PATH = "/batch/gmail/v1"

class KtorClient(
    private val dataStore: DataStore<Preferences>,
    private val authenticator: Authenticator,
) {
    private val refreshTokenKey = stringPreferencesKey("refreshToken")
    private val bearerTokenKey = stringPreferencesKey("bearerToken")

    private suspend fun processGranted(result: AuthResult.Granted): BearerTokens {
        val currentTokens = getTokens()
        val newTokens =
            BearerTokens(
                result.accessToken,
                result.refreshToken ?: currentTokens?.refreshToken,
            )
        storeTokens(newTokens)
        return newTokens
    }

    private suspend fun processLogin(result: AuthResult.Login): BearerTokens {
        val newTokens = exchangeCodeForTokens(result.code)
        storeTokens(newTokens)
        return newTokens
    }

    private val client =
        HttpClient {
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            Napier.d(message)
                        }
                    }

                level = LogLevel.HEADERS
            }
            install(Resources)
            defaultRequest {
                host = GOOGLE_APIS
                url {
                    protocol = URLProtocol.HTTPS
                }
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val currentTokens = getTokens()
                        Napier.d("Loading current tokens \n\t${currentTokens?.accessToken} \n\t${currentTokens?.refreshToken} ")
                        currentTokens
                    }

                    refreshTokens {
                        Napier.d("Refreshing tokens because 401 \n\t${oldTokens?.accessToken} \n\t${oldTokens?.refreshToken} ")
                        if (oldTokens?.refreshToken == null) {
                            Napier.d("Going for fresh auth.")
                            // Refresh token is missing ... Do the full auth flow
                            return@refreshTokens when (val result = authenticator.authenticate()) {
                                is AuthResult.Error -> {
                                    Napier.e("Could not authenticate", throwable = result.e)
                                    null
                                }

                                is AuthResult.Granted -> {
                                    Napier.d("Refresh authentication granted \n\t ${result.accessToken} \n\t${result.refreshToken}")
                                    val newTokens = processGranted(result)
                                    newTokens
                                }

                                is AuthResult.Login -> {
                                    Napier.d("Refresh authentication login \n\t ${result.code}")
                                    processLogin(result)
                                }
                            }
                        }

                        // Refresh tokens normally
                        Napier.d("Going for normal refresh with refresh token ${oldTokens?.refreshToken}")
                        val result =
                            client
                                .submitForm(
                                    url = "https://accounts.google.com/o/oauth2/token",
                                    formParameters =
                                        parameters {
                                            append("grant_type", "refresh_token")
                                            append("client_id", Keys.clientId())
                                            append("client_secret", Keys.clientSecret())
                                            append("refresh_token", oldTokens?.refreshToken ?: "")
                                        },
                                )
                        Napier.d("Refresh tokens result: ${result.bodyAsText()}")
                        if (result.status != HttpStatusCode.OK) {
                            Napier.e("Refresh token is invalid/expired. Doing full re-auth.")
                            // Refresh token is invalid/expired, do full auth flow
                            return@refreshTokens when (val authResult = authenticator.authenticate(forceAuthorization = true)) {
                                is AuthResult.Error -> {
                                    Napier.e("Could not authenticate", throwable = authResult.e)
                                    null
                                }

                                is AuthResult.Granted -> {
                                    Napier.d("Re-authentication granted \n\t ${authResult.accessToken} \n\t${authResult.refreshToken}")
                                    processGranted(authResult)
                                }

                                is AuthResult.Login -> {
                                    Napier.d("Re-authentication login \n\t ${authResult.code}")
                                    processLogin(authResult)
                                }
                            }
                        }

                        val refreshTokenInfo: TokenInfo = result.body()
                        val tokens = BearerTokens(refreshTokenInfo.accessToken, refreshTokenInfo.refreshToken ?: oldTokens?.refreshToken)
                        storeTokens(tokens)
                        tokens
                    }

                    sendWithoutRequest { request ->
                        request.url.host == GOOGLE_APIS
                    }
                }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = true
                    },
                )
            }
        }

    private suspend fun storeTokens(tokens: BearerTokens) {
        dataStore.edit { dataStore ->
            tokens.refreshToken?.let { dataStore[refreshTokenKey] = it }
            dataStore[bearerTokenKey] = tokens.accessToken
        }
    }

    private suspend fun getTokens(): BearerTokens? {
        var tokens: BearerTokens? = null

        dataStore.edit {
            val accessToken = it[bearerTokenKey] ?: return@edit
            val refreshToken = it[refreshTokenKey]
            tokens = BearerTokens(accessToken, refreshToken)
        }

        Napier.d("getTokens returned: ${tokens?.accessToken}, ${tokens?.refreshToken}")
        return tokens
    }

    private suspend fun exchangeCodeForTokens(code: String): BearerTokens {
        val response =
            client.submitForm(
                url = TOKEN_URI,
                formParameters =
                    parameters {
                        append("grant_type", "authorization_code")
                        append("code", code)
                        append("client_id", Keys.clientId())
                        append("client_secret", Keys.clientSecret())
                        append("redirect_uri", "http://127.0.0.1:8080")
                    },
            )

        val tokenInfo: TokenInfo = response.body()
        val bearerTokens = BearerTokens(tokenInfo.accessToken, tokenInfo.refreshToken)
        return bearerTokens
    }

    internal suspend inline fun <reified T : Any, reified R : ApiModel> getResource(
        resource: T,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): Result<R> {
        val response = client.get(resource) { builder() }
        return if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Napier.e("Error response: ${response.bodyAsText()}")
            Result.failure(Exception("Error ${response.status}: ${response.bodyAsText()}"))
        }
    }

    internal suspend inline fun <reified T : Any, reified R : ApiModel> postResource(
        resource: T,
        crossinline builder: HttpRequestBuilder.() -> Unit = {},
    ): Result<R> {
        val response = client.post(resource) { builder() }
        return if (response.status.isSuccess()) {
            Result.success(response.body())
        } else {
            Napier.e("Error response: ${response.bodyAsText()}")
            Result.failure(Exception("Error ${response.status}: ${response.bodyAsText()}"))
        }
    }
}
