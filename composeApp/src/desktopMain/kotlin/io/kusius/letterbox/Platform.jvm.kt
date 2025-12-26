package io.kusius.letterbox

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import java.io.File

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override fun debugBuild() {
    }
}

enum class DesktopOS {
    WINDOWS, MACOS, LINUX, UNKNOWN;

    companion object {
        fun current(): DesktopOS {
            val osName = System.getProperty("os.name").lowercase()
            return when {
                osName.contains("win") -> WINDOWS
                osName.contains("mac") -> MACOS
                osName.contains("linux") -> LINUX
                else -> UNKNOWN
            }
        }
    }
}

fun getAppDir(): File {
    val basePath = when(DesktopOS.current()) {
        DesktopOS.MACOS -> {
            "${System.getProperty("user.home")}/Library/Application Support"
        }
        DesktopOS.WINDOWS -> {
            System.getenv("APPDATA")
        }
        DesktopOS.LINUX -> {
            "${System.getProperty("user.home")}/.config"
        }
        DesktopOS.UNKNOWN -> {
            System.getProperty("java.io.tmpdir")
        }
    }

    return File(basePath).resolve("letterbox")
}
actual fun getPlatform(): Platform = JVMPlatform()
