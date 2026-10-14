package com.peto.ramap.domain.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 리뷰 조회 중 발생한 변경을 재생하고 새로고침에서도 펼쳐 본 범위를 복구한다. */
class LoadShopReviewsUseCase(
    private val repository: ReviewRepository,
    private val changes: ReviewChangeStore,
    private val reducer: ReviewChangeReducer,
) {
    private val cache = mutableMapOf<Pair<String, String?>, ShopReviewsPage>()
    private var cacheEpoch = changes.journal.value.epoch
    private var cacheRevision = changes.journal.value.revision

    fun findCached(
        shopId: String,
        viewerId: String?,
    ): ShopReviewsPage? {
        synchronizeCache()
        return cache[shopId to viewerId]
    }

    fun savePage(
        shopId: String,
        viewerId: String?,
        page: ShopReviewsPage,
    ) {
        synchronizeCache()
        cache[shopId to viewerId] = page
    }

    fun clearCache() {
        cache.clear()
    }

    private fun synchronizeCache() {
        val journal = changes.journal.value
        if (cacheEpoch != journal.epoch || journal.hasGapAfter(cacheRevision)) {
            cache.clear()
        } else {
            for (record in journal.changesAfter(cacheRevision)) {
                for ((key, page) in cache.toMap()) {
                    cache[key] = reducer.updateShopPage(page, record.change, key.first, key.second, page.hasMore)
                }
            }
        }
        cacheEpoch = journal.epoch
        cacheRevision = journal.revision
    }

    suspend operator fun invoke(
        shopId: String,
        viewerId: String?,
        retained: ShopReviewsPage,
        append: Boolean,
    ): RamapResult<ShopReviewsPage> {
        while (true) {
            currentCoroutineContext().ensureActive()
            val baseline = changes.journal.value
            val offset = if (append) retained.reviews.size.toLong() else 0L
            val result = repository.fetchShopReviewsPage(shopId, offset)
            if (result !is RamapResult.Success) return result
            var page = result.data
            if (!append) {
                while (page.reviews.size < retained.reviews.size && page.hasMore) {
                    val next = repository.fetchShopReviewsPage(shopId, page.reviews.size.toLong())
                    if (next !is RamapResult.Success) return next
                    val merged = reducer.appendShopPage(page.reviews, next.data, emptyList(), shopId, viewerId)
                    if (merged.reviews.size <= page.reviews.size) break
                    page = merged
                }
            }
            val journal = changes.journal.value
            if (baseline.epoch != journal.epoch) throw CancellationException(SESSION_CHANGED)
            if (journal.requiresShopReloadAfter(baseline.revision, shopId)) continue
            val events = journal.changesAfter(baseline.revision).map { it.change }
            val applied =
                if (append) {
                    reducer.appendShopPage(retained.reviews, page, events, shopId, viewerId)
                } else {
                    reducer.replayShopPage(page, retained.reviews, events, shopId, viewerId)
                }
            return RamapResult.Success(applied)
        }
    }

    companion object {
        private const val SESSION_CHANGED = "Review session changed during request"
    }
}
