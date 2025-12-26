package io.kusius.letterbox.data.stores

sealed interface MailsAction<out T> {
    data class Added<T>(
        val mails: List<T>,
    ) : MailsAction<T>

    data class Deleted<T>(
        val ids: List<String>,
    ) : MailsAction<T>

    class ToggleReadStatus<T>(
        val mailId: String,
    ) : MailsAction<T>

    class Delete<T>(
        val mailId: String,
    ) : MailsAction<T>
}
