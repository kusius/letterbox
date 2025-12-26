package io.kusius.letterbox.model

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

data class MailSummary(
    val id: String,
    val title: String,
    val sender: String,
    val summary: String,
    val receivedAtUnixMillis: Instant,
    val isRead: Boolean,
) : Model {
    companion object {
        fun randomMailSummary(id: Int): MailSummary =
            MailSummary(
                id = "mail$id",
                title = "Wow!! 😮 what an awesome mail title. This is some long ass mail title hey???" +
                        " What's up with this. I don't think this can fit the lines of the title",
                sender = "George",
                receivedAtUnixMillis = Clock.System.now(),
                summary = "Hello George, I hope this email ",
                isRead = Random.nextBoolean(),
            )
    }
}
