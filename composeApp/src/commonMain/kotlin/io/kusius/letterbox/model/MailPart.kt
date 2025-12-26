package io.kusius.letterbox.model

import kotlinx.serialization.Serializable

@Serializable
data class MailPart(
    val mimeType: MimeType,
    val body: MailPartBody?,
    val parts: List<MailPart>,
    val fileName: String? = null,
) : Model

@Serializable
data class MailPartBody(
    val attachmentId: String?,
    val size: Int,
    val data: String?,
)
