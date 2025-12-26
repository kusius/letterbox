package io.kusius.letterbox.data.stores

sealed interface MailAction<out T> {
    data class Add<T>(
        val mail: T,
    ) : MailAction<T>

    data class UpdateRead(
        val isRead: Boolean,
    ) : MailAction<Nothing>
}
