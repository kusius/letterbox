package io.kusius.letterbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.kusius.letterbox.data.network.AndroidDataStore
import io.kusius.letterbox.data.persistence.AndroidDriverFactory
import io.kusius.letterbox.domain.auth.AndroidAuthenticator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
