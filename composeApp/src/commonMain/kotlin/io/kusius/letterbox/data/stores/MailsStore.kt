@file:OptIn(ExperimentalStoreApi::class)

package io.kusius.letterbox.data.stores

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.kusius.letterbox.Database
import io.kusius.letterbox.HistoryEntity
import io.kusius.letterbox.MailEntity
import io.kusius.letterbox.data.network.ApiHistory
import io.kusius.letterbox.data.network.ApiMailRefs
import io.kusius.letterbox.data.network.ApiModifyMailLabels
import io.kusius.letterbox.data.network.ApiTrashMessage
import io.kusius.letterbox.data.network.KtorClient
import io.kusius.letterbox.data.network.KtorProvider
import io.kusius.letterbox.data.network.model.HistoryList
import io.kusius.letterbox.data.network.model.LabelModifyRequestBody
import io.kusius.letterbox.data.network.model.MessageRef
import io.kusius.letterbox.data.network.model.MessagesRefs
import io.kusius.letterbox.data.network.model.NetworkMail
import io.kusius.letterbox.data.persistence.DatabaseProvider
import io.kusius.letterbox.data.persistence.toModelList
import io.kusius.letterbox.model.MailSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

internal typealias LocalMailActions = List<MailsAction<MailEntity>>
internal typealias NetworkMailActions = List<MailsAction<NetworkMail>>
internal typealias OutputMailActions = List<MailsAction<MailSummary>>

const val LABEL_INBOX = "INBOX"
const val LABEL_UNREAD = "UNREAD"
const val LABEL_SPAM = "SPAM"

class MailsStore(
    private val delegate: MailsStoreFactory = MailsStoreFactory(),
) : MutableStore<String, OutputMailActions> by delegate.create()

class MailsStoreFactory(
    val client: KtorClient = KtorProvider.getInstance().client,
    val database: Database = DatabaseProvider.getInstance().database,
) {
    private val refreshMailsUseCase = RefreshMailsUseCase(database, client)

    fun create(): MutableStore<String, OutputMailActions> =
        MutableStoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth(),
                converter = createConverter(),
            ).build(
                updater = createUpdater(),
            )

    private fun createFetcher(): Fetcher<String, NetworkMailActions> =
        Fetcher.of { id ->
            refreshMailsUseCase().getOrThrow()
        }

    private fun createSourceOfTruth(): SourceOfTruth<String, LocalMailActions, OutputMailActions> =
        SourceOfTruth.of(
            reader = { _ ->
                database.mailEntityQueries.selectAll().asFlow().mapToList(Dispatchers.IO).map {
                    listOf(MailsAction.Added(it.toModelList()))
                }
            },
            writer = { mailId: String, mailActions: List<MailsAction<MailEntity>> ->
                Napier.i("Writer: called with ${mailActions.size} entities")
                mailActions.forEach { mailAction ->
                    when (mailAction) {
                        is MailsAction.Added -> {
                            database.transaction {
                                mailAction.mails.forEach { mail ->
                                    database.mailEntityQueries.insertMailEntity(mail)
                                }
                            }
                        }

                        is MailsAction.Deleted -> {
                            database.transaction {
                                mailAction.ids.forEach { id ->
                                    database.mailEntityQueries.deleteMail(id)
                                }
                            }
                        }

                        is MailsAction.ToggleReadStatus<*> -> {
                            database.transaction {
                                database.mailEntityQueries
                                    .getMail(mailAction.mailId)
                                    .executeAsOneOrNull()
                                    ?.let { current ->
                                        val newReadStatus = if (current.is_read == 1L) 0L else 1L
                                        database.mailEntityQueries.insertMailEntity(
                                            current.copy(is_read = newReadStatus),
                                        )
                                    }
                            }
                        }

                        is MailsAction.Delete<*> -> {
                            database.mailEntityQueries.deleteMail(mailAction.mailId)
                        }
                    }
                }
            },
        )

    private fun createConverter(): Converter<NetworkMailActions, LocalMailActions, OutputMailActions> =
        Converter
            .Builder<List<MailsAction<NetworkMail>>, List<MailsAction<MailEntity>>, List<MailsAction<MailSummary>>>()
            .fromOutputToLocal { it.toLocalMailActions() }
            .fromNetworkToLocal { it.toLocalMailActions() }
            .build()

    private fun createUpdater(): Updater<String, OutputMailActions, Boolean> =
        Updater.by(
            post = { key: String, mailActions: OutputMailActions ->
                var result: Result<*>? = null
                mailActions.forEach { action ->

                    when (action) {
                        is MailsAction.ToggleReadStatus -> {
                            val addedLabels = mutableListOf<String>()
                            val removedLabels = mutableListOf<String>()

                            // the current state is the source of truth, i.e the email has already changed its status
                            // in the DB after the user's toggle.
                            val current =
                                database.mailEntityQueries.getMail(key).executeAsOneOrNull()
                                    ?: return@by UpdaterResult.Error.Message("Mail not found in local store")
                            if (current.is_read == 1L) {
                                removedLabels.add(LABEL_UNREAD)
                            } else {
                                addedLabels.add(LABEL_UNREAD)
                            }
                            val r: Result<MessageRef> =
                                client.postResource(ApiModifyMailLabels(mailId = key)) {
                                    contentType(ContentType.Application.Json)
                                    setBody(
                                        LabelModifyRequestBody(
                                            addLabelIds = addedLabels,
                                            removeLabelIds = removedLabels,
                                        ),
                                    )
                                }

                            result = r
                        }

                        is MailsAction.Delete -> {
                            val r: Result<MessageRef> =
                                client.postResource(ApiTrashMessage(key))
                            result = r
                        }

                        else -> {
                            TODO()
                        }
                    }
                }

                result?.fold(
                    onSuccess = { UpdaterResult.Success.Untyped(true) },
                    onFailure = { UpdaterResult.Error.Exception(it) },
                ) ?: UpdaterResult.Error.Message("No result from actions $mailActions and key $key")
            },
            onCompletion = null,
        )
}
