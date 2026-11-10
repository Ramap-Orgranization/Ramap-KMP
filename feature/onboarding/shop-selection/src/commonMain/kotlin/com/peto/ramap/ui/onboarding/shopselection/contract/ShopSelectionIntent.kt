package com.peto.ramap.ui.onboarding.shopselection.contract

import com.peto.ramap.ui.base.Intent

public sealed interface ShopSelectionIntent : Intent {
    data class QueryChanged(
        val query: String,
    ) : ShopSelectionIntent

    data class ToggleBookmark(
        val shopId: String,
    ) : ShopSelectionIntent

    data object Search : ShopSelectionIntent

    data object RetryShops : ShopSelectionIntent

    data object LoadNextPage : ShopSelectionIntent

    data object RetryNextPage : ShopSelectionIntent

    data object RetryPersonalization : ShopSelectionIntent

    data object Complete : ShopSelectionIntent
}
