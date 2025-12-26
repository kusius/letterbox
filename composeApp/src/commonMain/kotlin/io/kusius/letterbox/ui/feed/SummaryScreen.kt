package io.kusius.letterbox.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.kusius.letterbox.model.MailSummary.Companion.randomMailSummary
import io.kusius.letterbox.ui.theme.AppTheme
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Serializable
object SummaryRoute

@Composable
fun SummaryScreenRoot(
    modifier: Modifier = Modifier,
    onNavigateToMail: (String) -> Unit,
    viewModel: SummaryScreenViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val effects = viewModel.sideEffects.collectAsStateWithLifecycle(null)
    when (val effect = effects.value) {
        is MailSummarySideEffect.NavigateToMail -> {
            onNavigateToMail(effect.mailId)
        }

        null -> {}
    }

    when (val state = uiState.value) {
        is MailSummaryUiState.Data -> {
            SummaryScreen(
                uiState = state,
                modifier = modifier.fillMaxSize(),
                onAction = viewModel::onAction,
            )
        }

        is MailSummaryUiState.Loading -> {
            Text(text = "Loading")
        }

        is MailSummaryUiState.Error -> {
            Text(text = "Error! ${state.throwable}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryScreen(
    uiState: MailSummaryUiState.Data,
    onAction: (MailSummaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        Text(text = "Unread Mail: ${uiState.numUnread}")
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onAction(MailSummaryAction.RefreshMails) },
            state = pullRefreshState,
        ) {
            LazyColumn(
                verticalArrangement =
                    Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterVertically,
                    ),
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
            ) {
                // Important to use the id key here because compose does not renew the captured item
                // in the lambda
                items(items = uiState.data, key = { it.id }) { item ->
                    SummaryItem(
                        item = item,
                        onAction = { onAction(MailSummaryAction.OpenMail(item.id)) },
                        onEndToStartSwipe = {
                            onAction(MailSummaryAction.Delete(item.id))
                        },
                        onStartToEndSwipe = {
                            onAction(MailSummaryAction.ToggleRead(item.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SummaryScreenPreview() {
    AppTheme {
        SummaryScreen(
            onAction = {},
            uiState =
                MailSummaryUiState.Data(
                    data =
                        buildList {
                            for (i in 1 until 15) {
                                add(randomMailSummary(i))
                            }
                        },
                ),
        )
    }
}
