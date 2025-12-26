package io.kusius.letterbox

import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sun.jna.Pointer
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBuilder
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.kusius.letterbox.platform.gestures.ScrollGestureDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

fun main() =
    application {
        Napier.base(DebugAntilog())

        System.setProperty("compose.interop.blending", "true")
        Window(
            onCloseRequest = ::exitApplication,
            title = "Letterbox",
        ) {
            val handleLong = window.windowHandle
            val nsWindowPtr = Pointer(handleLong)

            // Initialize gesture listener after window is created
            DisposableEffect(nsWindowPtr) {
                ScrollGestureDispatcher.initialize(window)

                onDispose {
                    // Cleanup if needed
                }
            }

            var restartRequired by remember { mutableStateOf(false) }
            var downloading by remember { mutableStateOf(0F) }
            var initialized by remember { mutableStateOf(false) }
            val download: KCEFBuilder.Download =
                remember {
                    KCEFBuilder.Download
                        .Builder()
                        .github()
                        .build()
                }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    KCEF.init(builder = {
                        installDir(File("kcef-bundle"))

                    /*
                      Add this code when using JDK 17.
                      Builder().github {
                          release("jbr-release-17.0.10b1087.23")
                      }.buffer(download.bufferSize).build()
                     */
                        progress {
                            onDownloading {
                                downloading = max(it, 0F)
                            }
                            onInitialized {
                                initialized = true
                            }
                        }
                        settings {
                            cachePath = File("cache").absolutePath
                        }
                    }, onError = {
                        it?.printStackTrace()
                    }, onRestartRequired = {
                        restartRequired = true
                    })
                }
            }

            if (restartRequired) {
                Text(text = "Restart required.")
            } else {
                if (initialized) {
                    App()
                } else {
                    Text(text = "Downloading $downloading%")
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    KCEF.disposeBlocking()
                }
            }
        }
    }
