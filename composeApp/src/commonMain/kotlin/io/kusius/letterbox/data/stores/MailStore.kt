@file:OptIn(ExperimentalStoreApi::class)

@file:Suppress("ktlint:standard:no-wildcard-imports")

package io.kusius.letterbox.data.stores

import app.cash.sqldelight.coroutines.asFlow
import io.github.aakira.napier.Napier
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.kusius.letterbox.Database
import io.kusius.letterbox.MailEntity
import io.kusius.letterbox.data.network.ApiGetAttachments
import io.kusius.letterbox.data.network.ApiMailRefs
import io.kusius.letterbox.data.network.ApiModifyMailLabels
import io.kusius.letterbox.data.network.KtorClient
import io.kusius.letterbox.data.network.KtorProvider
import io.kusius.letterbox.data.network.model.LabelModifyRequestBody
import io.kusius.letterbox.data.network.model.MessagePart
import io.kusius.letterbox.data.network.model.MessagePartBody
import io.kusius.letterbox.data.network.model.MessageRef
import io.kusius.letterbox.data.network.model.NetworkMail
import io.kusius.letterbox.data.persistence.DatabaseProvider
import io.kusius.letterbox.data.persistence.toEntity
import io.kusius.letterbox.data.persistence.toMail
import io.kusius.letterbox.data.stores.LocalMailType.Full
import io.kusius.letterbox.data.stores.LocalMailType.Summary
import io.kusius.letterbox.data.stores.MailAction.Add
import io.kusius.letterbox.data.stores.MailAction.UpdateRead
import io.kusius.letterbox.model.Mail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MutableStore
import org.mobilenativefoundation.store.store5.MutableStoreBuilder
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Updater
import org.mobilenativefoundation.store.store5.UpdaterResult

sealed interface LocalMailType {
    data class Summary(
        val summary: MailEntity,
    ) : LocalMailType

    data class Full(
        val summary: MailEntity,
    ) : LocalMailType
}

sealed interface NetworkMailType {
    data class Summary(
        val summary: NetworkMail,
    ) : NetworkMailType

    data class Full(
        val summary: NetworkMail,
        val attachmentData: List<MessagePartBody>,
    ) : NetworkMailType
}

private typealias LocalMailAction = MailAction<LocalMailType>
private typealias NetworkMailAction = MailAction<NetworkMailType>
private typealias OutputMailAction = MailAction<Mail>

class MailStore(
    private val delegate: MailStoreFactory = MailStoreFactory(),
) : MutableStore<String, OutputMailAction> by delegate.create()

class MailStoreFactory(
    val client: KtorClient = KtorProvider.getInstance().client,
    val database: Database = DatabaseProvider.getInstance().database,
) {
    fun create(): MutableStore<String, MailAction<Mail>> =
        MutableStoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth(),
                converter = createConverter(),
            ).build(updater = createUpdater())

    suspend fun fetchAttachmentsConcurrently(
        attachmentIds: List<String>,
        maxConcurrent: Int = 5,
        fetcher: suspend (String) -> MessagePartBody?, // replace with your own type
    ): List<MessagePartBody> =
        coroutineScope {
            val semaphore = Semaphore(maxConcurrent)

            val deferreds =
                attachmentIds.map { id ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            fetcher(id) // runs with concurrency limited to `maxConcurrent`
                        }
                    }
                }

            deferreds.awaitAll().filterNotNull()
        }

    // Recursive function to get all unresolved attachment ids for a MessagePart
    private fun getAttachmentIds(messagePart: MessagePart): List<String> {
        val attachmentIDs = mutableListOf<String>()
        if (messagePart.body?.data == null && messagePart.body!!.attachmentId != null) {
            attachmentIDs += messagePart.body.attachmentId
        }

        attachmentIDs +=
            messagePart.parts.flatMap {
                getAttachmentIds(it)
            }

        return attachmentIDs
    }

    private fun createFetcher(): Fetcher<String, NetworkMailAction> =
        Fetcher.of { id ->
            require(id.isNotEmpty()) {
                "Fetcher called with empty ID"
            }

            val resource = ApiMailRefs.ApiMail(id = id)
            val result: Result<NetworkMail> =
                client.getResource(resource) {
                    parameter("format", "full")
                }

            val mail =
                result.getOrThrow()
            val attachments = getAttachmentIds(mail.payload)

            val attachmentData =
                fetchAttachmentsConcurrently(attachments) { attachmentId ->
                    val result: Result<MessagePartBody> =
                        client.getResource(ApiGetAttachments(mail.id, attachmentId))
                    result
                        .onFailure {
                            Napier.e("Could not get attachment $id for mail $mail")
                        }.getOrNull()
                }

            Add(
                NetworkMailType.Full(
                    summary = mail,
                    attachmentData,
                ),
            )
        }

    private fun createSourceOfTruth(): SourceOfTruth<String, LocalMailAction, OutputMailAction> =
        SourceOfTruth.of(
            reader = { id ->
                database.mailEntityQueries
                    .getMail(id)
                    .asFlow()
                    .map { query ->
                        query
                            .executeAsOneOrNull()
                            ?.let { entity ->
                                if (entity.raw == null) {
                                    return@map null
                                }

                                Add(
                                    entity.toMail(),
                                )
                            }
                    }.distinctUntilChanged()
            },
            writer = { id: String, action: LocalMailAction ->
                when (action) {
                    is Add<LocalMailType> -> {
                        when (action.mail) {
                            is Summary -> {
                                database.mailEntityQueries.insertMailEntity(action.mail.summary)
                            }

                            is Full -> {
                                database.mailEntityQueries.insertMailEntity(action.mail.summary)
                            }
                        }
                    }

                    is UpdateRead -> {
                        TODO("Nothing to do for update read")
                    }
                }
            },
        )

    private fun createConverter(): Converter<NetworkMailAction, LocalMailAction, OutputMailAction> =
        Converter
            .Builder<NetworkMailAction, LocalMailAction, OutputMailAction>()
            .fromOutputToLocal {
                when (it) {
                    is Add<Mail> -> Add(Summary(it.mail.summary.toEntity()))
                    is UpdateRead -> TODO("No conversion for update read.")
                }
            }.fromNetworkToLocal {
                when (it) {
                    is Add<NetworkMailType> -> {
                        when (it.mail) {
                            is NetworkMailType.Summary -> {
                                val safeModel =
                                    it.mail.summary.toModel()
                                        ?: throw IllegalStateException("Could not convert network mail $it")
                                Add(Summary(safeModel.toEntity()))
                            }

                            is NetworkMailType.Full -> {
                                val safeModel =
                                    it.mail.summary.toFullMail()
                                        ?: throw IllegalStateException("Could not convert network mail $it")

                                Add(
                                    Full(
                                        summary = safeModel.toEntity(),
                                    ),
                                )
                            }
                        }
                    }

                    is UpdateRead -> {
                        throw IllegalStateException("No conversion for update read")
                    }
                }
            }.build()

    private fun createUpdater(): Updater<String, OutputMailAction, Boolean> =
        Updater.by(
            post = { key: String, action: OutputMailAction ->
                when (action) {
                    is Add<*> -> {}

                    is UpdateRead -> {
                        database.mailEntityQueries
                            .getMail(key)
                            .executeAsOneOrNull()
                            ?.let { localMail ->
                                val localReadStatus = if (localMail.is_read == 1L) true else false
                                if (localReadStatus == action.isRead) {
                                    return@by UpdaterResult.Success.Untyped(true)
                                }
                            }
                        val addedLabels = mutableListOf<String>()
                        val removedLabels = mutableListOf<String>()
                        if (action.isRead) {
                            removedLabels.add(LABEL_UNREAD)
                        } else {
                            addedLabels.add(LABEL_UNREAD)
                        }

                        val result: Result<MessageRef> =
                            client.postResource(ApiModifyMailLabels(mailId = key)) {
                                contentType(ContentType.Application.Json)
                                setBody(
                                    LabelModifyRequestBody(
                                        addLabelIds = addedLabels,
                                        removeLabelIds = removedLabels,
                                    ),
                                )
                            }
                        result.getOrNull()?.let { networkMail ->
                            database.mailEntityQueries.updateReadStatus(
                                is_read = if (action.isRead) 1L else 0L,
                                id = key,
                            )
                        }

                        return@by result.fold(
                            onSuccess = { UpdaterResult.Success.Untyped(true) },
                            onFailure = { UpdaterResult.Error.Exception(it) },
                        )
                    }
                }
                UpdaterResult.Success.Untyped(true)
            },
            onCompletion = null,
        )
}
