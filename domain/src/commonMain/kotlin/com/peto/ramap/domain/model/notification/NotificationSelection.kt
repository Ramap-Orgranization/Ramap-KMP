package com.peto.ramap.domain.model.notification

data class NotificationSelection internal constructor(
    val ids: Set<NotificationTypeId>,
) {
    fun contains(id: NotificationTypeId): Boolean = id in ids

    fun toggle(
        id: NotificationTypeId,
        catalog: NotificationCatalog,
    ): NotificationSelection = catalog.selection(if (contains(id)) ids - id else ids + id)

    fun hasNewSelectionsComparedWith(persisted: NotificationSelection): Boolean = (ids - persisted.ids).isNotEmpty()
}
