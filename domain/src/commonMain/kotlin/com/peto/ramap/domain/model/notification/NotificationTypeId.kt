package com.peto.ramap.domain.model.notification

import kotlin.jvm.JvmInline

@JvmInline
value class NotificationTypeId(
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }
}
