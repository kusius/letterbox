package io.kusius.letterbox.data.network.model

import io.kusius.letterbox.model.Model

sealed interface ApiModel {
    fun toModel(): Model? = null
}
