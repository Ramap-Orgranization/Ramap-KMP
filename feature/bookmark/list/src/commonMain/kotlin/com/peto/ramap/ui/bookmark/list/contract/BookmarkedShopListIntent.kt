package com.peto.ramap.ui.bookmark.list.contract

import com.peto.ramap.ui.base.Intent

sealed interface BookmarkedShopListIntent : Intent {
    data object OnRetry : BookmarkedShopListIntent

    data object LoadMore : BookmarkedShopListIntent

    data class OnRemovalConfirmed(
        val shopId: String,
    ) : BookmarkedShopListIntent
}
