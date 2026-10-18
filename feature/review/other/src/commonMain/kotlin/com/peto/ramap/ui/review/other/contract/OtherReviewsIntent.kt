package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.ui.base.Intent

sealed interface OtherReviewsIntent : Intent {
    data class OpenProfile(
        val userId: String,
    ) : OtherReviewsIntent

    data class SelectTab(
        val tab: OtherReviewsTab,
    ) : OtherReviewsIntent

    data object LoadMore : OtherReviewsIntent

    data object Retry : OtherReviewsIntent

    data object ToggleFollow : OtherReviewsIntent

    data object ToggleBlock : OtherReviewsIntent
}
