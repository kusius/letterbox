package io.kusius.letterbox.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kusius.letterbox.domain.MailDataSource
import io.kusius.letterbox.model.MailSummary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SummaryScreenViewModel(
    private val dataSource: MailDataSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow<MailSummaryUiState>(MailSummaryUiState.Loading)
    val uiState: StateFlow<MailSummaryUiState> = _uiState
    private val _sideEffects =
        MutableSharedFlow<MailSummarySideEffect>(
            replay = 0,
            extraBufferCapacity = 1,
        )
    val sideEffects: SharedFlow<MailSummarySideEffect> = _sideEffects

    init {
        viewModelScope.launch {
            loadMail()
        }
    }

    fun onAction(action: MailSummaryAction) {
        viewModelScope.launch {
            when (action) {
                is MailSummaryAction.OpenMail -> {
                    _sideEffects.emit(
                        MailSummarySideEffect.NavigateToMail(action.mailId),
                    )
                }

                MailSummaryAction.RefreshMails -> {
                    refreshMails()
                }

                is MailSummaryAction.ToggleRead -> {
                    dataSource.toggleReadStatus(action.mailId)
                }

                is MailSummaryAction.Delete -> {
                    dataSource.delete(action.mailId)
                }
            }
        }
    }

    private fun showError(e: Throwable) {
        _uiState.update {
            MailSummaryUiState.Error(e)
        }
    }

    private suspend fun refreshMails() {
        _uiState.update {
            if (it is MailSummaryUiState.Data) {
                it.copy(isRefreshing = true)
            } else {
                it
            }
        }
        dataSource.refreshMails()
    }

    private suspend fun loadMail() {
        _uiState.update { MailSummaryUiState.Loading }
        dataSource.getEmails().collect { newMails ->
            _uiState.update {
                newMails.toUiState()
            }
        }
    }

    private fun Result<List<MailSummary>>.toUiState(): MailSummaryUiState =
        fold(
            onSuccess = { MailSummaryUiState.Data(it) },
            onFailure = { MailSummaryUiState.Error(it) },
        )
}

sealed interface MailSummarySideEffect {
    data class NavigateToMail(
        val mailId: String,
    ) : MailSummarySideEffect
}

sealed interface MailSummaryUiState {
    object Loading : MailSummaryUiState

    data class Data(
        val data: List<MailSummary>,
        val numUnread: Int,
        val isRefreshing: Boolean,
    ) : MailSummaryUiState {
        constructor(data: List<MailSummary>) : this(
            data = data,
            numUnread = data.count { !it.isRead },
            isRefreshing = false,
        )
    }

    data class Error(
        val throwable: Throwable,
    ) : MailSummaryUiState
}

sealed interface MailSummaryAction {
    data class OpenMail(
        val mailId: String,
    ) : MailSummaryAction

    object RefreshMails : MailSummaryAction

    data class ToggleRead(
        val mailId: String,
    ) : MailSummaryAction

    data class Delete(
        val mailId: String,
    ) : MailSummaryAction
}
