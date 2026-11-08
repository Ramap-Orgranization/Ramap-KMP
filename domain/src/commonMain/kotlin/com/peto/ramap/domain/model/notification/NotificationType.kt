package com.peto.ramap.domain.model.notification

data class NotificationType(
    val id: NotificationTypeId,
    val title: String,
    val description: String,
    val displayOrder: Int,
)
