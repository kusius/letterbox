package io.kusius.letterbox.data.network.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock

internal class NetworkMapperTest {
    private val dummyMessagePart =
        MessagePart(
            partId = "partid",
            mimeType = "text/plain",
            headers =
                listOf(
                    Header(name = "From", value = "George <george@mail.com>"),
                    Header(name = "Subject", value = "Subject"),
                ),
        )
    val dummyNetworkMail =
        NetworkMail(
            id = "a",
            threadId = "thread",
            labelIds = emptyList(),
            snippet = "snippet",
            historyId = "1234",
            internalDate =
                Clock.System
                    .now()
                    .toEpochMilliseconds()
                    .toString(),
            payload = dummyMessagePart,
            sizeEstimate = 3,
        )

    @Test
    fun `a correct mail is mapped`() {
        val network = dummyNetworkMail

        val actual = network.toModel()

        assertNotNull(actual)
    }

    @Test
    fun `a mail must have a subject`() {
        val network =
            dummyNetworkMail.copy(
                payload = dummyMessagePart.copy(headers = emptyList()),
            )

        val actual = network.toModel()

        assertNull(actual)
    }

    @Test
    fun `mail is mapped correctly to name and address`() {
        val network =
            dummyNetworkMail

        val actual = network.toModel()

        assertNotNull(actual)
        assertEquals("George", actual.sender)
        assertEquals("george@mail.com", actual.senderEmail)
    }
}
