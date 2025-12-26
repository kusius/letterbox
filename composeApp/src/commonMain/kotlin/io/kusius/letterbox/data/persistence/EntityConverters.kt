@file:OptIn(ExperimentalSerializationApi::class)

package io.kusius.letterbox.data.persistence

import io.kusius.letterbox.MailEntity
import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailPart
import io.kusius.letterbox.model.MailSummary
import io.kusius.letterbox.model.MimeTypes
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.time.Instant

private val cbor = Cbor {}

internal fun MailEntity.toModel(): MailSummary =
    MailSummary(
        id = id,
        title = title,
        sender = sender,
        summary = summary ?: "",
        receivedAtUnixMillis = Instant.fromEpochMilliseconds(received_at_unix_millis),
        isRead = is_read == 1L,
    )

internal fun List<MailEntity>.toModelList(): List<MailSummary> = map(MailEntity::toModel)

internal fun MailSummary.toEntity(): MailEntity =
    MailEntity(
        id = id,
        title = title,
        sender = sender,
        summary = summary,
        received_at_unix_millis = receivedAtUnixMillis.toEpochMilliseconds(),
        is_read = if (isRead) 1L else 0L,
        raw = null,
    )

internal fun Mail.toEntity(): MailEntity = summary.toEntity().copy(raw = cbor.encodeToByteArray(mailPart))

internal fun MailEntity.toMail() =
    Mail(
        summary = toModel(),
        mailPart =
            if (raw == null) {
                MailPart(
                    mimeType = MimeTypes.Text.PLAIN,
                    body = null,
                    fileName = null,
                    parts = emptyList(),
                )
            } else {
                cbor.decodeFromByteArray<MailPart>(raw)
            },
    )
