package com.peto.ramap.data.model

import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import kotlinx.serialization.Serializable

@Serializable
internal data class OperatingNoticeFeedItemResponse(
    val notice: OperatingNoticeResponse,
    val shop: OperatingNoticeShopResponse,
) {
    fun toDomain(): OperatingNoticeFeedItem? {
        val resolved = notice.toDomain(shop.toDomain()) ?: return null
        return OperatingNoticeFeedItem(resolved)
    }
}
