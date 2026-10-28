package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedFilter
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.Review

interface ReviewFeedRepository {
    suspend fun fetchPage(
        cursor: ReviewFeedCursor?,
        sort: ReviewFeedSort = ReviewFeedSort.LATEST,
        filter: ReviewFeedFilter = ReviewFeedFilter.ALL,
    ): RamapResult<ReviewFeedPage>

    suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?>
}
