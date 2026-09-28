package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.WaitingSystem

data class ShopDetail(
    val shop: RamenShop,
    val likeCount: Long,
    val waitingSystem: WaitingSystem?,
    val event: ShopEvent?,
    val operatingNotice: OperatingNotice?,
    val operatingNotices: List<OperatingNotice> = operatingNotice?.let(::listOf).orEmpty(),
    val menuSections: List<MenuSection> = emptyList(),
    val menuUpdatedAt: String? = null,
    val reviews: List<Review> = emptyList(),
) {
    val reviewCount: Int get() = reviews.size
    val menuItemCount: Int get() = menuSections.sumOf { it.items.size }
}
