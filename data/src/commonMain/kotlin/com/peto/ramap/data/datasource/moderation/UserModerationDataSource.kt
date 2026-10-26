package com.peto.ramap.data.datasource.moderation

import com.peto.ramap.data.model.BlockedUserResponse

internal interface UserModerationDataSource {
    suspend fun fetchBlockedUsers(): List<BlockedUserResponse>

    suspend fun report(
        targetType: String,
        targetId: String,
        reason: String,
        details: String,
    )

    suspend fun changeBlock(
        userId: String,
        blocked: Boolean,
    )
}
