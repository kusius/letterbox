@file:OptIn(ExperimentalStoreApi::class)

package io.kusius.letterbox.data

import io.github.aakira.napier.Napier
import io.kusius.letterbox.data.stores.FullMailsKey
import io.kusius.letterbox.data.stores.FullMailsStore
import io.kusius.letterbox.data.stores.MailAction
import io.kusius.letterbox.data.stores.MailStore
import io.kusius.letterbox.data.stores.MailsAction
import io.kusius.letterbox.data.stores.MailsStore
import io.kusius.letterbox.domain.MailDataSource
import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse
import org.mobilenativefoundation.store.store5.StoreWriteRequest
import org.mobilenativefoundation.store.store5.impl.extensions.fresh

class MailRemoteLocalDataSource(
    val multiMailStore: MailsStore,
    val singleMailStore: MailStore,
    val fullMailsStore: FullMailsStore,
) : MailDataSource {
    override suspend fun getFullUnreadMails(): Flow<Result<List<Mail>>> =
        fullMailsStore
            .stream(StoreReadRequest.cached(key = FullMailsKey.AllUnread, refresh = true))
            .mapNotNull { response ->
                when (response) {
                    is StoreReadResponse.Data<List<Mail>> -> Result.success(response.value)
                    else -> null
                }
            }

    override suspend fun getEmails(page: Int): Flow<Result<List<MailSummary>>> =
        multiMailStore
            .stream<Boolean>(StoreReadRequest.cached(key = "", refresh = false))
            .mapNotNull { response ->
                when (response) {
                    is StoreReadResponse.Data<List<MailsAction<MailSummary>>> -> {
                        val data = response.value
                        val state =
                            data.flatMap { action ->
                                when (action) {
                                    is MailsAction.Added<MailSummary> -> action.mails
                                    else -> emptyList()
                                }
                            }

                        if (state.isEmpty()) {
                            null
                        } else {
                            Result.success(state)
                        }
                    }

                    is StoreReadResponse.Error.Exception -> {
                        Napier.e("Error.", throwable = response.error)
                        Result.failure(response.error)
                    }

                    //               is StoreReadResponse.Error.Custom<*> -> TODO()
//               is StoreReadResponse.Error.Custom<*> -> TODO()
//                is StoreReadResponse.Error.Message -> TODO()
//                StoreReadResponse.Initial -> TODO()
//                is StoreReadResponse.Loading -> TODO()
//                is StoreReadResponse.NoNewData -> TODO()
                    else -> {
                        null
                    }
                }
            }

    override suspend fun refreshMails() {
        try {
            multiMailStore.fresh<String, List<MailsAction<MailSummary>>, Boolean>("")
        } catch (e: Throwable) {
            Napier.e("Could not refresh emails", throwable = e)
        }
    }

    override suspend fun toggleReadStatus(mailId: String) {
        try {
            multiMailStore.write(
                StoreWriteRequest.of(
                    key = mailId,
                    value = listOf(MailsAction.ToggleReadStatus(mailId)),
                ),
            )
        } catch (e: Throwable) {
            Napier.e("Could not toggle read status.", e)
        }
    }

    override suspend fun updateReadStatus(
        mailId: String,
        isRead: Boolean,
    ) {
        try {
            singleMailStore.write(
                StoreWriteRequest.of(
                    key = mailId,
                    value = MailAction.UpdateRead(isRead),
                ),
            )
        } catch (e: Throwable) {
            Napier.e("Could not update red status.", e)
        }
    }

    override suspend fun delete(mailId: String) {
        try {
            multiMailStore.write(
                StoreWriteRequest.of(
                    key = mailId,
                    value = listOf(MailsAction.Delete(mailId)),
                ),
            )
        } catch (e: Throwable) {
            Napier.e("Could not delete mail with id $mailId", e)
        }
    }

    override suspend fun getMail(mailId: String): Flow<Result<Mail>> =
        singleMailStore
            .stream<Boolean>(
                StoreReadRequest.cached(
                    key = mailId,
                    refresh = false,
                ),
            ).mapNotNull { response ->
                when (response) {
                    is StoreReadResponse.Data<MailAction<Mail>> -> {
                        val action = response.value
                        when (action) {
                            is MailAction.Add<Mail> -> {
                                Result.success(action.mail)
                            }

                            is MailAction.UpdateRead -> {
                                // we dont emit when read status changed
                                null
                            }
                        }
                    }

                    //              is StoreReadResponse.Error.Custom<*> -> TODO()
                    //              is StoreReadResponse.Error.Exception -> TODO()
                    //              is StoreReadResponse.Error.Message -> TODO()
                    //              StoreReadResponse.Initial -> TODO()
                    //              is StoreReadResponse.Loading -> TODO()
                    //              is StoreReadResponse.NoNewData -> TODO()
                    else -> {
                        null
                    }
                }
            }
}
