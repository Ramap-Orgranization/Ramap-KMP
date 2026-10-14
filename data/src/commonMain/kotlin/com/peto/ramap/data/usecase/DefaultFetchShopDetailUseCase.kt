package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.domain.usecase.FetchShopDetailUseCase
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.domain.usecase.ShopDetailCacheLookup
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class DefaultFetchShopDetailUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val operatingNoticeRepository: OperatingNoticeRepository,
    private val reviewRepository: ReviewRepository,
    private val changes: ReviewChangeStore,
    private val reviewChangeReducer: ReviewChangeReducer,
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()
    private var cacheVersion = 0L
    private var epoch = changes.journal.value.epoch
    private var revision = changes.journal.value.revision

    /**
     * 매장 상세를 조회한다.
     *
     * 캐시된 매장·좋아요·웨이팅은 유지하고 서버의 최신 이벤트·공지·메뉴·리뷰를 반영한다.
     * 재검증 실패 시에는 캐시 전체를 그대로 사용한다.
     */
    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        synchronizeChanges()
        val baseline = changes.journal.value
        val requestedVersion = cacheVersion
        val cached = cache[shopId]
        if (cached != null) return revalidateDetail(cached, requestedVersion)

        val result = fetchDetailWithNoticesAndReviews(shopId)
        if (result is RamapResult.Success && requestedVersion == cacheVersion && baseline == changes.journal.value) {
            cache[result.data.shop.id] = result.data
        }
        return result
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup {
        synchronizeChanges()
        return cache[shopId]?.let(ShopDetailCacheLookup::Hit) ?: ShopDetailCacheLookup.Miss
    }

    override fun updateCachedReviews(
        shopId: String,
        page: ShopReviewsPage,
    ) {
        synchronizeChanges()
        cacheVersion++
        cache[shopId]?.let { detail ->
            val rows =
                if (detail.reviews.size > page.reviews.size) {
                    val updates = page.reviews.associateBy { it.id }
                    (detail.reviews.map { updates[it.id] ?: it } + page.reviews)
                        .distinctBy { it.id }
                        .sortedWith(compareByDescending<Review> { it.createdAt }.thenByDescending { it.id })
                } else {
                    page.reviews
                }
            cache[shopId] = detail.copy(reviews = rows, reviewCount = page.totalCount, hasReviewLoadFailure = false)
        }
    }

    private fun synchronizeChanges() {
        val journal = changes.journal.value
        if (epoch != journal.epoch || journal.hasGapAfter(revision)) {
            clearCache()
            epoch = journal.epoch
            revision = journal.revision
            return
        }
        for (record in journal.changesAfter(revision)) {
            val change = record.change
            for ((shopId, detail) in cache.toMap()) {
                if (change is ReviewChange.AccessChanged) {
                    if (detail.reviews.any { it.author.userId == change.userId }) cache.remove(shopId)
                    continue
                }
                val before = (change as? ReviewChange.Written)?.before?.review?.let { it.shopId == shopId && reviewChangeReducer.isShopVisible(change, it, journal.userId) } == true
                val after = (change as? ReviewChange.Written)?.after?.review?.let { it.shopId == shopId && reviewChangeReducer.isShopVisible(change, it, journal.userId) } == true
                cache[shopId] =
                    detail.copy(
                        reviews =
                            reviewChangeReducer.patch(detail.reviews, change, detail.reviews.size < detail.reviewCount) { receipt, review ->
                                review.shopId == shopId && reviewChangeReducer.isShopVisible(receipt, review, journal.userId)
                            },
                        reviewCount = (detail.reviewCount + (if (after) 1 else 0) - (if (before) 1 else 0)).coerceAtLeast(0),
                    )
            }
            cacheVersion++
            revision = record.revision
        }
    }

    override fun clearCache() {
        cacheVersion++
        cache.clear()
    }

    override fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        val likeCountDelta = if (enabled) 1L else -1L
        cache[shopId]?.let { detail ->
            cache[shopId] = detail.copy(likeCount = (detail.likeCount + likeCountDelta).coerceAtLeast(0L))
        }
    }

    /**
     * 캐시된 매장·웨이팅은 유지하고 이벤트·공지·메뉴·리뷰를 새로 조회해 상세를 갱신한다.
     */
    private suspend fun revalidateDetail(
        cached: ShopDetail,
        requestedVersion: Long,
    ): RamapResult<ShopDetail> {
        val baseline = changes.journal.value
        val refreshed =
            when (
                val result =
                    fetchDetailWithNoticesAndReviews(
                        shopId = cached.shop.id,
                        cachedReviews = cached.reviews,
                        cachedReviewCount = cached.reviewCount,
                    )
            ) {
                is RamapResult.Success -> result.data
                is RamapResult.Error -> return RamapResult.Success(cached)
            }
        val updated =
            cached.copy(
                event = refreshed.event,
                operatingNotice = refreshed.operatingNotice,
                operatingNotices = refreshed.operatingNotices,
                menuSections = refreshed.menuSections,
                menuUpdatedAt = refreshed.menuUpdatedAt,
                reviews = refreshed.reviews,
                reviewCount = refreshed.reviewCount,
                hasReviewLoadFailure = refreshed.hasReviewLoadFailure,
            )
        if (requestedVersion == cacheVersion && baseline == changes.journal.value) cache[cached.shop.id] = updated
        return RamapResult.Success(updated)
    }

    private suspend fun fetchDetailWithNoticesAndReviews(
        shopId: String,
        cachedReviews: List<Review> = emptyList(),
        cachedReviewCount: Int = cachedReviews.size,
    ): RamapResult<ShopDetail> {
        val detail = ramenShopRepository.fetchShopDetail(shopId)
        if (detail !is RamapResult.Success) return detail
        val notices = operatingNoticeRepository.fetchActiveShopOperatingNotices(shopId)
        val reviews = reviewRepository.fetchShopReviewsPage(shopId, offset = 0L)
        val detailWithNotices =
            if (notices is RamapResult.Success) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.of(SEOUL_TIME_ZONE))
                detail.data.copy(
                    operatingNotice =
                        notices.data
                            .filter { it.isRelevantForBusinessHoursAt(now) }
                            .sortedWith(compareByDescending<OperatingNotice> { it.businessDateAt(now) != null }.thenByDescending { it.updatedAt.orEmpty() }.thenBy { it.startDate }.thenBy { it.id })
                            .firstOrNull(),
                    operatingNotices = notices.data,
                )
            } else {
                detail.data
            }
        return RamapResult.Success(
            detailWithNotices.copy(
                reviews = if (reviews is RamapResult.Success) reviews.data.reviews else cachedReviews,
                reviewCount = if (reviews is RamapResult.Success) reviews.data.totalCount else cachedReviewCount,
                hasReviewLoadFailure = reviews is RamapResult.Error,
            ),
        )
    }

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
