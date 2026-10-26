package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.moderation.BlockedUser
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.repository.UserModerationRepository

class FakeUserModerationRepository(
    var blockedUsersResult: RamapResult<List<BlockedUser>> = RamapResult.Success(emptyList()),
) : UserModerationRepository {
    var unblockResult: RamapResult<Unit> = RamapResult.Success(Unit)
    val unblockedUserIds = mutableListOf<String>()

    override suspend fun fetchBlockedUsers(): RamapResult<List<BlockedUser>> = blockedUsersResult

    override suspend fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun reportUser(
        userId: String,
        reason: ReportReason,
        details: String,
    ): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun blockUser(userId: String): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun unblockUser(userId: String): RamapResult<Unit> {
        unblockedUserIds += userId
        if (unblockResult is RamapResult.Success) {
            val users = (blockedUsersResult as? RamapResult.Success)?.data.orEmpty()
            blockedUsersResult = RamapResult.Success(users.filterNot { it.profile.userId == userId })
        }
        return unblockResult
    }
}
