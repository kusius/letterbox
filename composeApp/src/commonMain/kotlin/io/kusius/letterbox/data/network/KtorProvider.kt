package io.kusius.letterbox.data.network

import io.kusius.letterbox.domain.auth.getAuthenticator

class KtorProvider private constructor(
    val client: KtorClient,
) {
    companion object {
        private var instance: KtorProvider? = null

        fun getInstance(): KtorProvider {
            if (instance == null) {
                instance =
                    KtorProvider(
                        KtorClient(
                            dataStore = getDataStore(),
                            authenticator = getAuthenticator(),
                        ),
                    )
            }

            return instance!!
        }
    }
}
