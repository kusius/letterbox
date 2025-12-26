package io.kusius.letterbox.data.stores

import io.github.aakira.napier.Napier
import io.ktor.client.request.parameter
import io.ktor.http.parameters
import io.kusius.letterbox.Database
import io.kusius.letterbox.HistoryEntity
import io.kusius.letterbox.data.network.ApiHistory
import io.kusius.letterbox.data.network.ApiMailRefs
import io.kusius.letterbox.data.network.KtorClient
import io.kusius.letterbox.data.network.model.HistoryList
import io.kusius.letterbox.data.network.model.MessagesRefs
import io.kusius.letterbox.data.network.model.NetworkMail

internal class RefreshMailsUseCase(
    val database: Database,
    val client: KtorClient,
) {
    suspend operator fun invoke(): Result<List<MailsAction<NetworkMail>>> {
        val lastHistoryId: Long? =
            database.historyEntityQueries.getLastHistoryId().executeAsOneOrNull()
        Napier.d("Current database history id: $lastHistoryId")
        return if (lastHistoryId != null) {
            partialFetch(lastHistoryId)
        } else {
            fullRefresh()
        }
    }

    private fun updateHistory(historyId: Long) {
        Napier.d("Adding history id: $historyId to DB")
        database.historyEntityQueries.insertHistoryEntity(HistoryEntity(historyId))
    }

    private suspend fun partialFetch(lastHistoryId: Long): Result<List<MailsAction<NetworkMail>>> {
        Napier.d("Partial refresh from history id: $lastHistoryId")
        val addedMails = mutableListOf<NetworkMail>()
        val deletedMailsIds = mutableListOf<String>()
        val allHistoryIds = mutableListOf<Long>()
        val touchedMailsIds = mutableSetOf<String>()
        var pageToken: String? = null
        var history: HistoryList

        do {
            val result: Result<HistoryList> =
                try {
                    client.getResource(ApiHistory()) {
                        parameter("startHistoryId", lastHistoryId)
                        if (pageToken != null) parameter("pageToken", pageToken)
                    }
                } catch (e: Throwable) {
                    throw e
                }

            history = result.getOrElse { return fullRefresh() }

            history.history?.forEach {
                Napier.d("Processing history with id: ${it.id}")
                for (newMessage in it.messagesAdded) {
                    // avoid fetching message details if already in db. If alterations where made,
                    // we will get them from the other lists of history
                    if (database.mailEntityQueries
                            .getMail(newMessage.message.id)
                            .executeAsOneOrNull() != null
                    ) {
                        continue
                    }

                    touchedMailsIds.add(newMessage.message.id)
                }

                it.messagesDeleted.forEach { deletedMessage ->
                    deletedMailsIds.add(deletedMessage.message.id)
                }

                it.labelsAdded.forEach { added ->
                    if (added.labelIds.contains(LABEL_SPAM)) {
                        // heal mails that have been marked as spam
                        deletedMailsIds.add(added.message.id)
                    } else {
                        touchedMailsIds.add(added.message.id)
                    }
                }

                it.labelsRemoved.forEach { removed ->
                    if (removed.labelIds.contains(LABEL_INBOX)) {
                        deletedMailsIds.add(removed.message.id)
                    } else {
                        touchedMailsIds.add(removed.message.id)
                    }
                }

                allHistoryIds.add(it.id.toLong())
            }

            pageToken = history.nextPageToken
        } while (pageToken != null)

        allHistoryIds.add(history.historyId.toLong())
        allHistoryIds.maxOrNull()?.let {
            updateHistory(it)
        }

        for (id in touchedMailsIds) {
            val messageResult: Result<NetworkMail> =
                client.getResource(ApiMailRefs.ApiMail(id = id)) {
                    parameters {
                        append("format", "full")
                    }
                }
            messageResult
                .onSuccess {
                    // Filter out spam mails
                    if (!it.labelIds.contains(LABEL_SPAM)) {
                        addedMails.add(it)
                    }
                }.onFailure {
                    Napier.e("Error while fetching message with id $id", throwable = it)
                }
        }

        return Result.success(
            listOf(
                MailsAction.Added(addedMails),
                MailsAction.Deleted(deletedMailsIds),
            ),
        )
    }

    private suspend fun fullRefresh(): Result<NetworkMailActions> {
        Napier.d("Full refresh!")
        var maxHistoryId = -1L
        val result: Result<MessagesRefs> = client.getResource(ApiMailRefs())
        val messagesRefs = result.getOrThrow()

        val messageSummaries: List<NetworkMail> =
            messagesRefs.messages.mapNotNull { it ->
                val messageResult: Result<NetworkMail> =
                    client.getResource(ApiMailRefs.ApiMail(id = it.id)) {
                        parameters {
                            append("format", "full")
                        }
                    }
                messageResult
                    .onSuccess {
                        it.historyId.toLongOrNull()?.let { historyId ->
                            if (historyId > maxHistoryId) maxHistoryId = historyId
                        }
                    }.onFailure { e ->
                        Napier.e("Error while ful data refresh", throwable = e)
                    }.getOrNull()
            }

        if (maxHistoryId > 0L) {
            updateHistory(maxHistoryId)
        } else {
            Napier.e("Could not get last history id (max was $maxHistoryId). Messages: $messageSummaries")
        }

        return Result.success(listOf(MailsAction.Added(messageSummaries)))
    }
}
