package io.kusius.letterbox.data.stores

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.ktor.client.request.parameter
import io.kusius.letterbox.Database
import io.kusius.letterbox.HistoryEntity
import io.kusius.letterbox.MailEntity
import io.kusius.letterbox.data.network.ApiMailRefs
import io.kusius.letterbox.data.network.KtorClient
import io.kusius.letterbox.data.network.KtorProvider
import io.kusius.letterbox.data.network.model.MessagesRefsSimple
import io.kusius.letterbox.data.network.model.NetworkMail
import io.kusius.letterbox.data.persistence.DatabaseProvider
import io.kusius.letterbox.data.persistence.toEntity
import io.kusius.letterbox.data.persistence.toMail
import io.kusius.letterbox.model.Mail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Converter
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

internal typealias Output = List<Mail>
internal typealias Local = List<MailEntity>
internal typealias Network = List<NetworkMail>

sealed interface FullMailsKey {
    object AllUnread : FullMailsKey
}

class FullMailsStore(
    private val delegate: FullMailsStoreFactory = FullMailsStoreFactory(),
) : Store<FullMailsKey, Output> by delegate.create()

class FullMailsStoreFactory(
    val client: KtorClient = KtorProvider.getInstance().client,
    val database: Database = DatabaseProvider.getInstance().database,
) {
    fun create(): Store<FullMailsKey, Output> =
        StoreBuilder
            .from(
                fetcher = createFetcher(),
                sourceOfTruth = createSourceOfTruth(),
                converter = createConverter(),
            ).build()

    private fun createFetcher(): Fetcher<FullMailsKey, Network> =
        Fetcher.of { key ->
            when (key) {
                FullMailsKey.AllUnread -> {
                    val response: Result<MessagesRefsSimple> =
                        client.getResource(ApiMailRefs()) {
                            parameter("labelIds", LABEL_UNREAD)
                            parameter("labelIds", LABEL_INBOX)
                        }
                    val unreadRefs =
                        response.getOrElse {
                            Napier.e("Could not get response", it)
                            throw it
                        }

                    val idsToFetch =
                        unreadRefs.messages.mapNotNull {
                            // if we have body for this mail we wont fetch it again.
                            val local = database.mailEntityQueries.getMail(it.id).executeAsOneOrNull()
                            if (local == null || local.raw == null) {
                                it.id
                            } else {
                                null
                            }
                        }

                    val networkMails =
                        idsToFetch.mapNotNull { id ->
                            val response: Result<NetworkMail> =
                                client.getResource(ApiMailRefs.ApiMail(id = id)) {
                                    parameter("format", "full")
                                }

                            response.fold(
                                onSuccess = { networkMail ->
                                    networkMail
                                },
                                onFailure = {
                                    Napier.d("Could not fetch full mail", it)
                                    null
                                },
                            )
                        }

                    val updateHistoryId = networkMails.maxOfOrNull { it.historyId.toLong() } ?: 0L
                    database.historyEntityQueries.insertHistoryEntity(HistoryEntity(updateHistoryId))

                    Napier.d("Fetched new ${networkMails.size} unread mails")
                    networkMails
                }
            }
        }

    private fun createSourceOfTruth(): SourceOfTruth<FullMailsKey, Local, Output> =
        SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    FullMailsKey.AllUnread -> {
                        database.mailEntityQueries
                            .selectAllUnread()
                            .asFlow()
                            .mapToList(Dispatchers.IO)
                            .map { rows ->
                                // If DB completely empty, only trigger a full fetch when we have never
                                // recorded a history id (meaning we never synced before). If history exists
                                // but there are zero unread mails, the user has legitimately read everything
                                // and we should emit an empty list instead of forcing repeated fetches.
                                if (rows.isEmpty()) {
                                    val lastHistory = database.historyEntityQueries.getLastHistoryId().executeAsOneOrNull()
                                    if (lastHistory == null) {
                                        // No history recorded -> we've never synced; trigger fetch
                                        null
                                    } else {
                                        // History exists -> no unread mails -> emit empty list
                                        emptyList<Mail>()
                                    }
                                } else {
                                    rows
                                        .takeIf { it.all { mail -> mail.raw != null } }
                                        ?.map(MailEntity::toMail)
                                }
                            }
                    }
                }
            },
            writer = { _, local ->
                database.transaction {
                    local.forEach {
                        database.mailEntityQueries.insertMailEntity(it)
                    }
                }
            },
        )

    private fun createConverter(): Converter<Network, Local, Output> =
        Converter
            .Builder<Network, Local, Output>()
            .fromOutputToLocal { it.map { mail -> mail.toEntity() } }
            .fromNetworkToLocal { it.mapNotNull { mail -> mail.toFullMail()?.toEntity() } }
            .build()
}
