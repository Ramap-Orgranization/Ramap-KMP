package com.peto.ramap.ui.main.feed

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.feed.ReviewFeedCursor
import com.peto.ramap.domain.model.feed.ReviewFeedPage
import com.peto.ramap.domain.model.feed.ReviewFeedSort
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.ReviewFeedRepository

internal class FakeReviewFeedRepository : ReviewFeedRepository {
    val requests = mutableListOf<Pair<ReviewFeedSort, ReviewFeedCursor?>>()
    var response: suspend (ReviewFeedCursor?, ReviewFeedSort) -> RamapResult<ReviewFeedPage> = { _, _ ->
        RamapResult.Success(ReviewFeedPage(emptyList(), null))
    }

    override suspend fun fetchPage(
        cursor: ReviewFeedCursor?,
        sort: ReviewFeedSort,
    ): RamapResult<ReviewFeedPage> {
        requests += sort to cursor
        return response(cursor, sort)
    }

    override suspend fun fetchBlockedReviewOnce(reviewId: String): RamapResult<Review?> = RamapResult.Success(null)
}
