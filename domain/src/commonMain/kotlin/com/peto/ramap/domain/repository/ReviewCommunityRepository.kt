package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import kotlinx.coroutines.flow.Flow

interface ReviewCommunityRepository {
    fun observeChanges(): Flow<Unit>

    suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?>

    suspend fun fetchPublicProfile(userId: String): RamapResult<PublicProfile?>

    suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>>

    suspend fun fetchBlockedUsers(): RamapResult<List<PublicProfile>>

    suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String = "",
    ): RamapResult<Unit>

    suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String = "",
    ): RamapResult<Unit>

    suspend fun blockUser(userId: String): RamapResult<Unit>

    suspend fun unblockUser(userId: String): RamapResult<Unit>
}
