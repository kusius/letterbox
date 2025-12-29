package io.kusius.letterbox.ui.mail

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.kusius.letterbox.ui.Route
import io.kusius.letterbox.ui.common.MailItem
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data class MailRoute(
    val mailId: String,
) : Route

@Composable
fun MailScreenRoot(
    mailId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MailScreenViewModel = koinViewModel(),
) {
    LaunchedEffect(mailId) {
        viewModel.onAction(MailAction.Load(mailId))
    }

    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState.value) {
        is MailUiState.Data -> {
            MailScreen(
                state,
                modifier = modifier,
                onAction = {
                    viewModel.onAction(it)
                },
                onBack = onBack,
            )
        }

        is MailUiState.Error -> {
            Error(state, modifier)
        }

        MailUiState.Loading -> {
            Loading(modifier)
        }
    }
}

@Composable
private fun MailScreen(
    data: MailUiState.Data,
    onAction: (MailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MailItem(data.mail)
    }
}

@Composable
private fun Loading(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Loading")
    }
}

@Composable
private fun Error(
    error: MailUiState.Error,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Error: $error")
    }
}
