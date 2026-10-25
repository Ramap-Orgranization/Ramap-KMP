package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.shop.RamenShop

data class MyReview(
    val review: Review,
    val shopName: String,
    val shop: RamenShop? = null,
    val moderationNote: String? = null,
)
