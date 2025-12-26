@file:OptIn(ExperimentalCoroutinesApi::class)

package io.kusius.letterbox.ui.mail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.kusius.letterbox.domain.MailDataSource
import io.kusius.letterbox.model.Mail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MailScreenViewModel(
    val dataSource: MailDataSource,
) : ViewModel() {
    private val mailId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MailUiState> =
        mailId
            .filterNotNull()
            .flatMapLatest {
                dataSource.getMail(it)
            }.map { result ->
                result.fold(
                    onSuccess = { mail -> MailUiState.Data(mail) },
                    onFailure = { error -> MailUiState.Error(error) },
                )
            }.distinctUntilChangedBy {
                if (it is MailUiState.Data) {
                    it.mail.mailPart
                } else {
                    it
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(1_000),
                initialValue = MailUiState.Loading,
            )

    fun onAction(action: MailAction) {
        viewModelScope.launch {
            when (action) {
                MailAction.Delete -> {
                    TODO()
                }

                MailAction.MarkRead -> {
                    mailId.value?.let {
                        dataSource.updateReadStatus(mailId = it, isRead = true)
                    }
                }

                is MailAction.Load -> {
                    Napier.d("load")
                    mailId.value = action.mailId
                    onAction(MailAction.MarkRead)
                }

                is MailAction.FetchAttachment -> {
                    Napier.d("Fetch attachment ${action.attachmentId} for mail ${mailId.value}")
                }
            }
        }
    }
}

sealed interface MailUiState {
    object Loading : MailUiState

    data class Data(
        val mail: Mail,
    ) : MailUiState

    data class Error(
        val throwable: Throwable,
    ) : MailUiState
}

sealed interface MailAction {
    data class Load(
        val mailId: String,
    ) : MailAction

    object MarkRead : MailAction

    object Delete : MailAction

    data class FetchAttachment(
        val attachmentId: String,
    ) : MailAction
}
