package com.peto.ramap.ui.review.search.contract

import com.peto.ramap.ui.base.SideEffect

sealed interface ReviewShopSearchSideEffect : SideEffect {
    data class WriteReview(
        val shopId: String,
    ) : ReviewShopSearchSideEffect
}
