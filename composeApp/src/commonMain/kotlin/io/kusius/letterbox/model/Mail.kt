package io.kusius.letterbox.model

data class Mail(
    val summary: MailSummary,
    val mailPart: MailPart,
) : Model
