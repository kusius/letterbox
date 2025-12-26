package io.kusius.letterbox.data.network

import Letterbox.composeApp.BuildConfig

const val TOKEN_URI = "https://accounts.google.com/o/oauth2/token"
const val AUTH_URI = "https://accounts.google.com/o/oauth2/auth"

// These are read from secrets.properties (not in source control).
// build.gradle.kts(:composeApp) does the reading.
object Keys {
    fun clientId(): String = BuildConfig.CLIENT_ID

    fun clientSecret(): String = BuildConfig.CLIENT_SECRET
}
