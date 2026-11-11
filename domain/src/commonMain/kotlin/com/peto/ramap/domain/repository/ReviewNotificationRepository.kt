package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult

interface ReviewNotificationRepository {
    suspend fun fetchEnabled(accountId: String): RamapResult<Boolean>

    suspend fun updateEnabled(
        accountId: String,
        enabled: Boolean,
    ): RamapResult<Boolean>

    suspend fun updateReviewEnabled(
        reviewId: String,
        enabled: Boolean,
    ): RamapResult<Boolean>
}
