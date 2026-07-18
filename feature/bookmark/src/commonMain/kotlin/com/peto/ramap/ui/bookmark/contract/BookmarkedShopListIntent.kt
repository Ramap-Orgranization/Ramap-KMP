package com.peto.ramap.ui.bookmark.contract

import com.peto.ramap.ui.base.Intent

sealed interface BookmarkedShopListIntent : Intent {
    data object Retry : BookmarkedShopListIntent

    data class OnRemovalConfirmed(
        val shopId: String,
    ) : BookmarkedShopListIntent
}
