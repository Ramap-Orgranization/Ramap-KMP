package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.fake.FakeShopReviewRepository

internal class FakeArchiveReviewRepository : ReviewRepository by FakeShopReviewRepository() {
    val deletedReviewIds = mutableListOf<String>()
    var delete: suspend (String) -> RamapResult<Unit> = { RamapResult.Success(Unit) }

    override suspend fun deleteReview(reviewId: String): RamapResult<Unit> {
        deletedReviewIds += reviewId
        return delete(reviewId)
    }
}
