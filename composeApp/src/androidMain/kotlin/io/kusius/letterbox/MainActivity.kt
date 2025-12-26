package io.kusius.letterbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kusius.letterbox.data.network.AndroidDataStore
import io.kusius.letterbox.data.persistence.AndroidDriverFactory
import io.kusius.letterbox.domain.auth.AndroidAuthenticator

private val log = KotlinLogging.logger { }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Napier.base(DebugAntilog())
        Napier.i("Starting application")

        AndroidDriverFactory.initialize(this)
        AndroidDataStore.initialize(this)
        AndroidAuthenticator.initialize(this)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    App()
}
