package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReviewRepository
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
) : FetchShopDetailUseCase {
    private val cache = mutableMapOf<String, ShopDetail>()
    private var cacheVersion = 0L

    /**
     * 매장 상세를 조회한다.
     *
     * 캐시된 매장·좋아요·웨이팅은 유지하고 서버의 최신 이벤트·공지·메뉴·리뷰를 반영한다.
     * 재검증 실패 시에는 캐시 전체를 그대로 사용한다.
     */
    override suspend fun invoke(shopId: String): RamapResult<ShopDetail> {
        val requestedVersion = cacheVersion
        val cached = cache[shopId]
        if (cached != null) return revalidateDetail(cached, requestedVersion)

        val result = fetchDetailWithNoticesAndReviews(shopId)
        if (result is RamapResult.Success && requestedVersion == cacheVersion) {
            cache[result.data.shop.id] = result.data
        }
        return result
    }

    override fun findCached(shopId: String): ShopDetailCacheLookup =
        cache[shopId]
            ?.let(ShopDetailCacheLookup::Hit)
            ?: ShopDetailCacheLookup.Miss

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
            )
        if (requestedVersion == cacheVersion) cache[cached.shop.id] = updated
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
                    operatingNotice = notices.data.firstOrNull { it.isCurrentOrScheduledAt(now) },
                    operatingNotices = notices.data,
                )
            } else {
                detail.data
            }
        return RamapResult.Success(
            detailWithNotices.copy(
                reviews = if (reviews is RamapResult.Success) reviews.data.reviews else cachedReviews,
                reviewCount = if (reviews is RamapResult.Success) reviews.data.totalCount else cachedReviewCount,
            ),
        )
    }

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
