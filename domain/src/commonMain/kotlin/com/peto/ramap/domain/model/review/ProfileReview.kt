package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop

data class ProfileReview(
    val review: Review,
    val shop: RamenShop,
)
