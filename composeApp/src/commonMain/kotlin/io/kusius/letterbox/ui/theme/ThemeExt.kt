package io.kusius.letterbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorScheme.successContainer: Color
    get() = Color(0xFFBEEFBB) // Green 90

val ColorScheme.onSuccessContainer: Color
    get() = Color(0xFF00522C) // Green 30

object AppTheme {
    @Composable
    fun colorScheme(darkTheme: Boolean = isSystemInDarkTheme()) =
        when {
            darkTheme -> darkColorScheme
            else -> lightColorScheme
        }

    @Composable
    fun typography() = MaterialTheme.typography
}
