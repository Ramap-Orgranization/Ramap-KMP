package com.peto.ramap.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.peto.ramap.domain.model.review.MyReviewsPage

class MyReviewsPagePreviewParameterProvider : PreviewParameterProvider<MyReviewsPage> {
    private val myReviews = MyReviewPreviewParameterProvider().myReviewPreviewSamples.take(2)

    override val values: Sequence<MyReviewsPage> =
        sequenceOf(
            MyReviewsPage(
                reviews = myReviews,
                totalCount = myReviews.size,
                publicCount = myReviews.count { it.isPublic },
                privateCount = myReviews.count { !it.isPublic },
                profileIsPublic = true,
            ),
        )
}
