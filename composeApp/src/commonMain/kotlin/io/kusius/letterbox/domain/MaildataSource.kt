package io.kusius.letterbox.domain

import io.kusius.letterbox.model.Mail
import io.kusius.letterbox.model.MailSummary
import kotlinx.coroutines.flow.Flow

interface MailDataSource {
    suspend fun getEmails(page: Int = 0): Flow<Result<List<MailSummary>>>

    suspend fun getFullUnreadMails(): Flow<Result<List<Mail>>>

    suspend fun refreshMails()

    suspend fun toggleReadStatus(mailId: String)

    suspend fun updateReadStatus(
        mailId: String,
        isRead: Boolean,
    )

    suspend fun delete(mailId: String)

    suspend fun getMail(mailId: String): Flow<Result<Mail>>
}
