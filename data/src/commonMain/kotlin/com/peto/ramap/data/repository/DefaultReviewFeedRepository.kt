package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.feed.FeedDataSource
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewFeedRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultReviewFeedRepository(
    private val reviews: FeedDataSource,
) : ReviewFeedRepository {
    override suspend fun fetchPage(
        cursor: ReviewFeedCursor?,
        sort: ReviewFeedSort,
    ): RamapResult<ReviewFeedPage> =
        invokeRequest {
            val page = reviews.fetchReviewsPage(cursor, sort)
            ReviewFeedPage(
                reviews = page.reviews.map { it.toDomain() },
                nextCursor = page.nextCursor(),
            )
        }

    override suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?> =
        invokeRequest {
            reviews.fetchBlockedReviewOnce(reviewId)?.toDomain()
        }
}
