package com.peto.ramap.ui.review.search.contract

import com.peto.ramap.ui.base.Intent

sealed interface ReviewShopSearchIntent : Intent {
    data class QueryChanged(
        val query: String,
    ) : ReviewShopSearchIntent

    data object Search : ReviewShopSearchIntent

    data object Retry : ReviewShopSearchIntent

    data class SelectShop(
        val shopId: String,
    ) : ReviewShopSearchIntent
}
