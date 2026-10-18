package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.store.ReviewChangeStore

/** 매장과 열람 사용자별 페이지를 보관하며 변경 기록에 따라 갱신하거나 무효화한다. */
class ShopReviewCache(
    private val changes: ReviewChangeStore,
    private val reducer: ReviewChangeReducer,
) {
    private val pages = mutableMapOf<ShopReviewCacheKey, ShopReviewsPage>()
    private var epoch = changes.journal.value.epoch
    private var revision = changes.journal.value.revision

    fun find(key: ShopReviewCacheKey): ShopReviewsPage? {
        synchronize()
        return pages[key]
    }

    fun save(
        key: ShopReviewCacheKey,
        page: ShopReviewsPage,
    ) {
        synchronize()
        pages[key] = page
    }

    fun clear() {
        pages.clear()
    }

    private fun synchronize() {
        val journal = changes.journal.value
        if (epoch != journal.epoch || journal.hasGapAfter(revision)) {
            pages.clear()
        } else {
            replayChanges(journal)
        }
        epoch = journal.epoch
        revision = journal.revision
    }

    private fun replayChanges(journal: ReviewChangeJournal) {
        for (record in journal.changesAfter(revision)) {
            for (entry in pages.entries) {
                val key = entry.key
                val page = entry.value
                entry.setValue(reducer.updateShopPage(page, record.change, key.shopId, key.viewerId, page.hasMore))
            }
        }
    }
}
