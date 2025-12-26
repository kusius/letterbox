package io.kusius.letterbox

interface Platform {
    val name: String

    fun debugBuild()
}

expect fun getPlatform(): Platform
