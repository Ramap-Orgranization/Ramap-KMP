package com.peto.ramap.domain.model.notification

data class NotificationSettings(
    val accountId: String,
    val shopId: String,
    val catalog: NotificationCatalog,
    val selection: NotificationSelection,
    val hasHiddenEffectiveSubscription: Boolean = false,
) {
    val hasEffectiveSubscription: Boolean get() = selection.ids.isNotEmpty() || hasHiddenEffectiveSubscription
}
