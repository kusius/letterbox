package io.kusius.letterbox.domain.auth

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import io.kusius.letterbox.getAppDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import letterbox.composeapp.generated.resources.Res
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader

private class DesktopAuthenticator private constructor(
    private val clientSecretsPath: String,
    private val tokenStorePath: String,
) : Authenticator {
    private val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
    private var credential: Credential? = null

    private suspend fun getCredential(forceRefresh: Boolean = false): Credential =
        withContext(Dispatchers.IO) {
            if (credential != null && !forceRefresh) {
                return@withContext credential!!
            }

            // Clear the token store directory when forcing refresh to ensure fresh auth
            if (forceRefresh) {
                val tokenStoreDir = File(tokenStorePath)
                if (tokenStoreDir.exists()) {
                    tokenStoreDir.deleteRecursively()
                }
                credential = null
            }

            val clientSecrets = loadClientSecrets()
            val scopes =
                listOf(
                    "https://www.googleapis.com/auth/gmail.readonly",
                    "https://www.googleapis.com/auth/gmail.modify",
                )

            val flow =
                GoogleAuthorizationCodeFlow
                    .Builder(
                        NetHttpTransport(),
                        jsonFactory,
                        clientSecrets,
                        scopes,
                    ).setDataStoreFactory(FileDataStoreFactory(File(tokenStorePath)))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build()

            val receiver =
                LocalServerReceiver
                    .Builder()
                    .setPort(8080)
                    .build()

            credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
            credential!!
        }

    private suspend fun loadClientSecrets(): GoogleClientSecrets {
        val bytes = Res.readBytes("files/credentials.json")
        val inputStream = ByteArrayInputStream(bytes)

        return GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))
    }

    override suspend fun authenticate(forceAuthorization: Boolean): AuthResult =
        try {
            val credential = getCredential(forceRefresh = forceAuthorization)

            val accessToken =
                credential.accessToken ?: return AuthResult.Error(
                    Exception("No access token available"),
                )

            val refreshToken = credential.refreshToken

            AuthResult.Granted(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        } catch (e: Exception) {
            AuthResult.Error(e)
        }

    override suspend fun revoke() =
        withContext(Dispatchers.IO) {
            try {
                credential?.let {
//                it.expireAccessToken()
//                it.revokeToken()
                }
                credential = null

                val tokenStoreDir = File(tokenStorePath)
                if (tokenStoreDir.exists()) {
                    tokenStoreDir.deleteRecursively()
                }
            } catch (e: Exception) {
                // Log but don't throw - revoke is best effort
                e.printStackTrace()
            }
        }

    companion object {
        private var instance: DesktopAuthenticator? = null

        fun getInstance(
            clientSecretsPath: String = "credentials.json",
            tokenStorePath: String = getAppDir().resolve("tokens").absolutePath,
        ): DesktopAuthenticator {
            if (instance == null) {
                instance = DesktopAuthenticator(clientSecretsPath, tokenStorePath)
            }
            return instance!!
        }
    }
}

actual fun getAuthenticator(): Authenticator = DesktopAuthenticator.getInstance()
