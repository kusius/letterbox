package io.kusius.letterbox

interface Platform {
    val name: String

    fun setupLogging()
}

expect fun getPlatform(): Platform
