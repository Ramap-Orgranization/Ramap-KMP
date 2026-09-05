package com.peto.ramap.domain.usecase

import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopReview
import com.peto.ramap.domain.model.shop.WaitingSystem

data class ShopDetail(
    val shop: RamenShop,
    val likeCount: Long,
    val waitingSystem: WaitingSystem?,
    val event: ShopEvent?,
    val operatingNotice: OperatingNotice?,
    val menuSections: List<MenuSection> = emptyList(),
    val menuUpdatedAt: String? = null,
    val reviews: List<ShopReview> = emptyList(),
)
