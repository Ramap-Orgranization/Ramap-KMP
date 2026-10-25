package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.CommunityFeedCursor
import com.peto.ramap.domain.model.community.CommunityFeedPage
import com.peto.ramap.domain.model.review.Review

interface ReviewFeedRepository {
    suspend fun fetchPage(cursor: CommunityFeedCursor?): RamapResult<CommunityFeedPage>

    suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?>
}
