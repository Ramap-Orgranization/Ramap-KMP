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
            val result = repository.fetchShopReviewsPage(shopId, offset)
            if (result !is RamapResult.Success) return result
            var page = result.data
            if (!isAppending) {
                while (page.reviews.size < targetSize && page.hasMore) {
                    val next = repository.fetchShopReviewsPage(shopId, page.reviews.size.toLong())
                    if (next !is RamapResult.Success) return next
                    val merged = reducer.appendShopPage(page.reviews, next.data, emptyList(), shopId, viewerId)
                    if (merged.reviews.size <= page.reviews.size) break
                    page = merged
                }
            }
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
