package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.PublicSavedShopsPage
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.review.Review
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun observeChanges(): Flow<Unit>

    suspend fun fetchMyCommunityProfile(): RamapResult<PublicProfile?>

    suspend fun fetchProfileAccess(userId: String): RamapResult<ProfileAccess>

    suspend fun fetchUserReviews(
        userId: String,
        offset: Long,
    ): RamapResult<List<Review>>

    suspend fun fetchUserSavedShops(
        userId: String,
        offset: Long,
    ): RamapResult<PublicSavedShopsPage>

    suspend fun fetchBlockedUsers(): RamapResult<List<BlockedUser>>

    suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String = DEFAULT_REPORT_DETAILS,
    ): RamapResult<Unit>

    suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String = DEFAULT_REPORT_DETAILS,
    ): RamapResult<Unit>

    suspend fun blockUser(userId: String): RamapResult<Unit>

    suspend fun unblockUser(userId: String): RamapResult<Unit>

    companion object {
        const val DEFAULT_REPORT_DETAILS: String = ""
    }
}
