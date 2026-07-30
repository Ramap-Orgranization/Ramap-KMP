package com.peto.ramap.domain.model.rank

import com.peto.ramap.domain.model.shop.RamenShop

data class ShopRanking(
    val shop: RamenShop,
    val likeCount: Long,
)
