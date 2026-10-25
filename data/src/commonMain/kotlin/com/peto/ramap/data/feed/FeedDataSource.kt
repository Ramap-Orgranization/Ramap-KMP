package com.peto.ramap.data.feed

import com.peto.ramap.data.model.CommunityFeedPageResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.community.CommunityFeedCursor

internal interface FeedDataSource {
    suspend fun fetchCommunityReviewsPage(cursor: CommunityFeedCursor?): CommunityFeedPageResponse

    suspend fun fetchCommunityBlockedReviewOnce(reviewId: String): ReviewResponse?
}
