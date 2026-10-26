package com.peto.ramap.data.feed

import com.peto.ramap.data.model.ReviewFeedPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedSort

internal interface FeedDataSource {
    suspend fun fetchReviewsPage(
        cursor: ReviewFeedCursor?,
        sort: ReviewFeedSort,
    ): ReviewFeedPageResponse

    suspend fun fetchBlockedReviewOnce(reviewId: String): ReviewResponse?
}
