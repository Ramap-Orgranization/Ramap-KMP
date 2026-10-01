package com.peto.ramap.ui.review.my.contract

import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.ui.base.Intent

sealed interface MyReviewsIntent : Intent {
    data class SelectFilter(
        val filter: MyReviewVisibility,
    ) : MyReviewsIntent

    data object Retry : MyReviewsIntent

    data object LoadMore : MyReviewsIntent

    data class DeleteReview(
        val reviewId: String,
    ) : MyReviewsIntent
}
