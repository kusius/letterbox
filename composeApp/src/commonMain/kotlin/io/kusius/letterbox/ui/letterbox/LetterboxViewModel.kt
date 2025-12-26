package io.kusius.letterbox.ui.letterbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.kusius.letterbox.domain.MailDataSource
import io.kusius.letterbox.model.Mail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LetterboxUiState {
    data class Data(
        val data: List<Mail>,
    ) : LetterboxUiState

    object Loading : LetterboxUiState

    data class Error(
        val error: Throwable,
    ) : LetterboxUiState
}

sealed class LetterboxAction(
    open val mail: Mail,
) {
    data class MarkRead(
        override val mail: Mail,
    ) : LetterboxAction(mail)

    data class Archive(
        override val mail: Mail,
    ) : LetterboxAction(mail)
}

class LetterboxViewModel(
    private val dataSource: MailDataSource,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LetterboxUiState>(LetterboxUiState.Loading)
    val uiState: StateFlow<LetterboxUiState> =
        _uiState
            .map {
                if (it is LetterboxUiState.Data) {
                    Napier.d("${it.data.size} mails")
                }
                it
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), LetterboxUiState.Loading)

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    fun onAction(action: LetterboxAction) {
        viewModelScope.launch {
            when (action) {
                is LetterboxAction.Archive -> dataSource.delete(action.mail.summary.id)
                is LetterboxAction.MarkRead -> dataSource.updateReadStatus(action.mail.summary.id, true)
            }
        }
    }

    private suspend fun loadData() {
        dataSource.getFullUnreadMails().collect { result ->
            _uiState.update {
                result.fold(
                    onSuccess = {
                        Napier.d("Emitting ${it.size} unread mails")
                        LetterboxUiState.Data(data = it)
                    },
                    onFailure = { LetterboxUiState.Error(error = it) },
                )
            }
        }
    }
}
