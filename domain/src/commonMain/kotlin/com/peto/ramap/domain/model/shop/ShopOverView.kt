package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice

data class ShopOverView(
    val shop: RamenShop,
    val likeCount: Long,
    val waitingSystem: WaitingSystem?,
    val event: ShopEvent?,
    val operatingNotice: OperatingNotice?,
    val operatingNotices: List<OperatingNotice> = operatingNotice?.let(::listOf).orEmpty(),
) {
    fun updateBookmarkCount(enabled: Boolean): ShopOverView = copy(likeCount = (likeCount + if (enabled) 1L else -1L).coerceAtLeast(0L))
}
