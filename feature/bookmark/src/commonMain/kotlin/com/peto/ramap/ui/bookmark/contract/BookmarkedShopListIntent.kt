package com.peto.ramap.ui.bookmark.contract

import com.peto.ramap.ui.base.Intent

sealed interface BookmarkedShopListIntent : Intent {
    data class OnRemovalConfirmed(
        val shopId: String,
    ) : BookmarkedShopListIntent
}
