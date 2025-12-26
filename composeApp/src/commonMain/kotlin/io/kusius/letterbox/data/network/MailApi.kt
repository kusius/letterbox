package io.kusius.letterbox.data.network

import io.ktor.resources.Resource

@Resource("/gmail/v1/users/me/messages")
class ApiMailRefs {
    @Resource("{id}")
    class ApiMail(
        val parent: ApiMailRefs = ApiMailRefs(),
        val id: String,
        val parameters: Map<String, String> = emptyMap(),
    )
}

@Resource("/gmail/v1/users/me/history")
class ApiHistory

@Resource("/gmail/v1/users/me/messages/{mailId}/modify")
class ApiModifyMailLabels(
    val mailId: String,
)

@Resource("/gmail/v1/users/me/profile")
class ApiUserProfile

@Resource("/gmail/v1/users/me/messages/{id}/trash")
class ApiTrashMessage(
    val id: String,
)

@Resource("/gmail/v1/users/me/messages/{messageId}/attachments/{id}")
class ApiGetAttachments(
    val messageId: String,
    val id: String,
)
