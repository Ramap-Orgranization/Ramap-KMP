package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.review.ReviewNotificationDataSource
import com.peto.ramap.domain.repository.ReviewNotificationRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultReviewNotificationRepository(
    private val dataSource: ReviewNotificationDataSource,
) : ReviewNotificationRepository {
    override suspend fun fetchEnabled(accountId: String) = invokeRequest { dataSource.fetchEnabled(accountId) }

    override suspend fun updateEnabled(
        accountId: String,
        enabled: Boolean,
    ) = invokeRequest { dataSource.updateEnabled(accountId, enabled) }

    override suspend fun updateReviewEnabled(
        reviewId: String,
        enabled: Boolean,
    ) = invokeRequest { dataSource.updateReviewEnabled(reviewId, enabled) }
}
