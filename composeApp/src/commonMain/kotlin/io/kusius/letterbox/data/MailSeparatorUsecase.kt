package io.kusius.letterbox.data

object MailSeparatorUseCase {
    val emailSeparatorRegex = Regex("""^(.*?)\s*<([^>]+)>$""")
}
