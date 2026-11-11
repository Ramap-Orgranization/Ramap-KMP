package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.ReviewNotificationRepository

class FakeReviewNotificationRepository(
    private var enabled: Boolean = false,
) : ReviewNotificationRepository {
    val enabledUpdates = mutableListOf<Boolean>()
    val reviewUpdates = mutableListOf<Pair<String, Boolean>>()

    override suspend fun fetchEnabled(accountId: String): RamapResult<Boolean> = RamapResult.Success(enabled)

    override suspend fun updateEnabled(
        accountId: String,
        enabled: Boolean,
    ): RamapResult<Boolean> {
        enabledUpdates += enabled
        this.enabled = enabled
        return RamapResult.Success(enabled)
    }

    override suspend fun updateReviewEnabled(
        reviewId: String,
        enabled: Boolean,
    ): RamapResult<Boolean> {
        reviewUpdates += reviewId to enabled
        return RamapResult.Success(enabled)
    }
}
