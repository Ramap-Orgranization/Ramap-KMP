package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.shop.AdministrativeArea
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeFeedItemResponse(
    val notice: OperatingNoticeResponse,
    val shop: OperatingNoticeShopResponse,
    val area: String? = null,
    val sigungu: String? = null,
) {
    fun toDomain(): OperatingNoticeFeedItem? {
        val resolved = notice.toDomain(shop.toDomain()) ?: return null
        return OperatingNoticeFeedItem(resolved, AdministrativeArea.entries.firstOrNull { it.name.equals(area, true) }, sigungu)
    }
}
