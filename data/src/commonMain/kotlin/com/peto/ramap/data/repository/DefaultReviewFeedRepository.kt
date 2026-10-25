package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.feed.FeedDataSource
import com.peto.ramap.domain.model.community.CommunityFeedCursor
import com.peto.ramap.domain.model.community.CommunityFeedPage
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewFeedRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultReviewFeedRepository(
    private val reviews: FeedDataSource,
) : ReviewFeedRepository {
    override suspend fun fetchPage(cursor: CommunityFeedCursor?): RamapResult<CommunityFeedPage> =
        invokeRequest {
            val page = reviews.fetchCommunityReviewsPage(cursor)
            CommunityFeedPage(
                reviews = page.reviews.map { it.toDomain() },
                nextCursor = page.nextCursor(),
            )
        }

    override suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?> =
        invokeRequest {
            reviews.fetchCommunityBlockedReviewOnce(reviewId)?.toDomain()
        }
}
