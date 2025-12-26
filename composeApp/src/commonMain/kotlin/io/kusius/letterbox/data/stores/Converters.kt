package io.kusius.letterbox.data.stores

import io.kusius.letterbox.data.network.model.NetworkMail
import io.kusius.letterbox.data.persistence.toEntity
import io.kusius.letterbox.data.stores.MailsAction.Added
import io.kusius.letterbox.data.stores.MailsAction.Delete
import io.kusius.letterbox.data.stores.MailsAction.Deleted
import io.kusius.letterbox.data.stores.MailsAction.ToggleReadStatus
import io.kusius.letterbox.model.MailSummary
import kotlin.jvm.JvmName

@JvmName("outputToLocalMailActions")
internal fun OutputMailActions.toLocalMailActions(): LocalMailActions =
    map { out ->
        when (out) {
            is MailsAction.Added<MailSummary> -> Added(out.mails.map(MailSummary::toEntity))
            is MailsAction.Deleted -> Deleted(ids = out.ids)
            is MailsAction.ToggleReadStatus<MailSummary> -> ToggleReadStatus(out.mailId)
            is MailsAction.Delete<*> -> Delete(out.mailId)
        }
    }

@JvmName("networkToLocalMailActions")
internal fun NetworkMailActions.toLocalMailActions(): LocalMailActions =
    map { net ->
        when (net) {
            is Added<NetworkMail> -> {
                Added(
                    net.mails.mapNotNull {
                        it.toModel()?.toEntity()
                    },
                )
            }

            is Deleted -> {
                Deleted(ids = net.ids)
            }

            is ToggleReadStatus<*> -> {
                ToggleReadStatus(net.mailId)
            }

            is Delete<*> -> {
                Delete(net.mailId)
            }
        }
    }
