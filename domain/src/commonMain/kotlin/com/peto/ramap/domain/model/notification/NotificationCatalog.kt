package com.peto.ramap.domain.model.notification

class NotificationCatalog(
    val revision: Long,
    types: List<NotificationType>,
) {
    val types: List<NotificationType> = types.sortedWith(compareBy({ it.displayOrder }, { it.id.value }))
    private val ids = types.mapTo(mutableSetOf()) { it.id }

    init {
        require(ids.size == types.size)
    }

    fun selection(ids: Set<NotificationTypeId>): NotificationSelection {
        require(this.ids.containsAll(ids))
        return NotificationSelection(ids.toSet())
    }

    fun selectAll(): NotificationSelection = selection(ids)

    fun clear(): NotificationSelection = selection(emptySet())
}
