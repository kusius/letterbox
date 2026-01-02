package io.kusius.letterbox.data.network.model

import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailPart
import io.kusius.letterbox.model.MailPartBody
import io.kusius.letterbox.model.MailSummary
import io.kusius.letterbox.model.MimeTypes.asMimeType
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.time.Instant

@Serializable
data class MessagesRefs(
    val messages: List<MessageRef>,
    val nextPageToken: String,
    val resultSizeEstimate: Int,
) : ApiModel

@Serializable
data class MessagesRefsSimple(
    val messages: List<MessageRef>,
) : ApiModel

@Serializable
data class MessageRef(
    val id: String,
    val threadId: String,
) : ApiModel

@Serializable
data class NetworkRawMailContent(
    val id: String,
    val threadId: String,
    val labelIds: List<String>,
    val snippet: String,
    val historyId: String,
    val internalDate: String,
    val sizeEstimate: Int,
    val raw: String,
) : ApiModel

@Serializable
data class NetworkMail(
    val id: String,
    val threadId: String,
    val labelIds: List<String>,
    val snippet: String,
    val historyId: String,
    val internalDate: String,
    val payload: MessagePart,
    val sizeEstimate: Int,
) : ApiModel {
    override fun toModel(): MailSummary? {
        val sender = getHeaderOrNull("From") ?: return null
        val match = emailSeparatorRegex.matchEntire(sender) ?: return null
        val groups = match.groupValues.takeIf { it.size == 3 } ?: return null

        return MailSummary(
            id = id,
            title = getHeaderOrNull("Subject") ?: return null,
            sender = groups[1],
            senderEmail = groups[2],
            summary = snippet,
            receivedAtUnixMillis =
                internalDate.toLongOrNull()?.let {
                    Instant.fromEpochMilliseconds(it)
                }
                    ?: return null,
            isRead = !hasLabel("UNREAD"),
        )
    }

    fun toFullMail(): Mail? {
        return Mail(
            summary = this.toModel() ?: return null,
            mailPart = payload.toModel(),
        )
    }

    private companion object {
        val emailSeparatorRegex = Regex("""^(.*?)\s*<([^>]+)>$""")
    }
}

private fun NetworkMail.getHeaderOrNull(headerName: String): String? = payload?.headers?.firstOrNull { it.name == headerName }?.value

private fun NetworkMail.hasLabel(labelName: String): Boolean = labelIds.contains(labelName)

@Serializable
data class MessagePart(
    val partId: String,
    val mimeType: String,
    val fileName: String? = null,
    val headers: List<Header>, // RFC 2822
    val body: MessagePartBody? = null,
    val parts: List<MessagePart> = emptyList(),
) : ApiModel {
    override fun toModel(): MailPart =
        MailPart(
            mimeType = mimeType.asMimeType(),
            fileName = fileName,
            body =
                body?.let {
                    MailPartBody(
                        attachmentId = it.attachmentId,
                        size = it.size,
                        data =
                            it.data?.let { data ->
                                val string = Base64.UrlSafe.decode(data).decodeToString()
                                string
                            },
                    )
                },
            parts = parts.map(MessagePart::toModel),
        )
}

@Serializable
data class Header(
    val name: String,
    val value: String,
) : ApiModel

@Serializable
data class MessagePartBody(
    val attachmentId: String? = null,
    val size: Int = 0,
    val data: String? = null,
) : ApiModel

@Serializable
data class HistoryList(
    val history: List<History>? = null,
    val nextPageToken: String? = null,
    val historyId: String,
) : ApiModel

@Serializable
data class History(
    val id: String,
    val messages: List<MessageRef> = emptyList(),
    val messagesAdded: List<MessageAdded> = emptyList(),
    val messagesDeleted: List<MessageDeleted> = emptyList(),
    val labelsAdded: List<LabelModified> = emptyList(),
    val labelsRemoved: List<LabelModified> = emptyList(),
)

@Serializable
data class MessageAdded(
    val message: MessageRef,
)

@Serializable
data class LabelModified(
    val message: MessageRef,
    val labelIds: List<String>,
)

@Serializable
data class MessageDeleted(
    val message: MessageRef,
)

@Serializable
data class LabelModifyRequestBody(
    val addLabelIds: List<String> = emptyList(),
    val removeLabelIds: List<String> = emptyList(),
)

@Serializable
data class UserProfile(
    val emailAddress: String,
    val messagesTotal: Int,
    val threadsTotal: Int,
    val historyId: String,
) : ApiModel
