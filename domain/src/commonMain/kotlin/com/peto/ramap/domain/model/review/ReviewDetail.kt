package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.shop.RamenShop

data class ReviewDetail(
    val review: Review,
    val shopName: String,
    val canLike: Boolean,
    val canWriteComments: Boolean,
    val shop: RamenShop? = null,
    val canShare: Boolean = false,
    val reviewNotificationsEnabled: Boolean? = null,
)
