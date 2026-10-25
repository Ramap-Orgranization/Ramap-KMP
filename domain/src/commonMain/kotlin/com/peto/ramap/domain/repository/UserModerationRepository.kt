package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.community.BlockedUser
import com.peto.ramap.domain.model.community.ReportReason

interface UserModerationRepository {
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
