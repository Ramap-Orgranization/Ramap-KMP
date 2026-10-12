package com.peto.ramap.domain.model.review.change

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop

data class ReviewSnapshot(
    val review: Review,
    val shopName: String,
    val moderationNote: String? = null,
    val shop: RamenShop? = null,
)
