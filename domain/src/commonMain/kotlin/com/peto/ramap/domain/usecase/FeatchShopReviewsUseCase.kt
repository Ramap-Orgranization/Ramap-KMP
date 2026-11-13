package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.ShopReviewCacheKey
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 리뷰 조회 중 발생한 변경을 재생하고 새로고침에서도 펼쳐 본 범위를 복구한다. */
class FeatchShopReviewsUseCase(
    private val repository: ReviewRepository,
    private val changes: ReviewChangeStore,
    private val reducer: ReviewChangeReducer,
    private val cache: ShopReviewCache,
    private val refreshReviews: RefreshReviewListUseCase,
) {
    fun findCached(
        shopId: String,
        viewerId: String?,
    ): ShopReviewsPage? = cache.find(ShopReviewCacheKey(shopId, viewerId))

    fun savePage(
        shopId: String,
        viewerId: String?,
        page: ShopReviewsPage,
    ) {
        cache.save(ShopReviewCacheKey(shopId, viewerId), page)
    }

    fun clearCache() {
        cache.clear()
    }

    suspend operator fun invoke(
        shopId: String,
        viewerId: String?,
        retained: ShopReviewsPage,
        append: Boolean,
    ): RamapResult<ShopReviewsPage> {
        var retainedPage = retained
        var isAppending = append
        var targetSize = retained.reviews.size
        while (true) {
            currentCoroutineContext().ensureActive()
            val baseline = changes.journal.value
            val offset = if (isAppending) retainedPage.reviews.size.toLong() else 0L
            val seenReviewIds = mutableSetOf<String>()
            val result =
                refreshReviews.fetchExtent(
                    start = offset,
                    reviewCount = if (isAppending) 0 else targetSize,
                    fetchPage = { pageOffset -> repository.fetchShopReviewsPage(shopId, requireNotNull(pageOffset)) },
                    countReviews = { page -> page.reviews.count { seenReviewIds.add(it.id) } },
                    nextCursor = { _, page -> seenReviewIds.size.toLong().takeIf { seenReviewIds.size < page.totalCount } },
                )
            val pages =
                when (result) {
                    is RamapResult.Error -> return result
                    is RamapResult.Success -> result.data.pages
                }
            val page = pages.last().copy(reviews = pages.flatMap { it.reviews }.distinctBy { it.id })
            val journal = changes.journal.value
            if (baseline.epoch != journal.epoch) throw CancellationException(SESSION_CHANGED)
            val events = journal.changesAfter(baseline.revision).map { it.change }
            if (journal.requiresShopReloadAfter(baseline.revision, shopId)) {
                if (isAppending) targetSize += page.reviews.size
                retainedPage = reducer.replayShopPage(retainedPage, retainedPage.reviews, events, shopId, viewerId)
                isAppending = false
                continue
            }
            val applied =
                if (isAppending) {
                    reducer.appendShopPage(retainedPage.reviews, page, events, shopId, viewerId)
                } else {
                    reducer.replayShopPage(page, retainedPage.reviews, events, shopId, viewerId)
                }
            return RamapResult.Success(applied)
        }
    }

    companion object {
        private const val SESSION_CHANGED = "Review session changed during request"
    }
}
