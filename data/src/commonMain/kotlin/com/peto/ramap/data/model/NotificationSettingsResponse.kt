package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.notification.NotificationType
import com.peto.ramap.domain.model.notification.NotificationTypeId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NotificationSettingsResponse(
    @SerialName("account_id") val accountId: String,
    @SerialName("shop_id") val shopId: String,
    @SerialName("catalog_revision") val catalogRevision: Long,
    val types: List<NotificationTypeResponse>,
    @SerialName("selected_type_ids") val selectedTypeIds: Set<String>,
    @SerialName("has_hidden_effective_subscription") val hasHiddenEffectiveSubscription: Boolean = false,
) {
    fun toDomain(): NotificationSettings {
        val activeTypes =
            types.filter { it.isActive && it.isVisible }.map {
                NotificationType(NotificationTypeId(it.id), it.title, it.description, it.displayOrder)
            }
        val catalog = NotificationCatalog(catalogRevision, activeTypes)
        val validSelectedIds =
            selectedTypeIds
                .map(::NotificationTypeId)
                .filterTo(mutableSetOf()) { id -> activeTypes.any { it.id == id } }
        return NotificationSettings(
            accountId = accountId,
            shopId = shopId,
            catalog = catalog,
            selection = catalog.selection(validSelectedIds),
            hasHiddenEffectiveSubscription = hasHiddenEffectiveSubscription,
        )
    }
}
