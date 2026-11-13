package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class RefreshReviewListUseCase(
    private val reducer: ReviewChangeReducer,
) {
    fun <Row> retainGuestReviews(
        previousViewerId: String?,
        viewerId: String?,
        reviews: List<Row>,
        reviewOf: (Row) -> Review,
    ): List<Row> {
        if (previousViewerId != null || viewerId == null) return emptyList()
        return reviews.filter { row ->
            val review = reviewOf(row)
            !review.isBlocked && reducer.isPublicPublished(review)
        }
    }

    suspend fun <Cursor, Page> fetchExtent(
        start: Cursor?,
        reviewCount: Int,
        fetchPage: suspend (Cursor?) -> RamapResult<Page>,
        countReviews: (Page) -> Int,
        nextCursor: (Cursor?, Page) -> Cursor?,
    ): RamapResult<Extent<Cursor, Page>> {
        val pages = mutableListOf<Page>()
        var loadedCount = 0
        var cursor = start
        do {
            val previous = cursor
            val result = fetchPage(previous)
            currentCoroutineContext().ensureActive()
            val page =
                when (result) {
                    is RamapResult.Error -> return result
                    is RamapResult.Success -> result.data
                }
            pages += page
            loadedCount += countReviews(page)
            cursor = nextCursor(previous, page).takeUnless { it == previous }
        } while (loadedCount < reviewCount && cursor != null)
        return RamapResult.Success(Extent(pages, cursor))
    }

    data class Extent<Cursor, Page>(
        val pages: List<Page>,
        val nextCursor: Cursor?,
    )
}
