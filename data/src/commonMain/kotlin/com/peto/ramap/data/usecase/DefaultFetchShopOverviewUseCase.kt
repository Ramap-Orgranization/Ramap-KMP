package com.peto.ramap.data.usecase

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.ShopOverviewCacheLookup
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

internal class DefaultFetchShopOverviewUseCase(
    private val ramenShopRepository: RamenShopRepository,
    private val operatingNoticeRepository: OperatingNoticeRepository,
) : FetchShopOverviewUseCase {
    private val cache = mutableMapOf<String, ShopOverView>()
    private var generation = 0L

    override suspend fun invoke(shopId: String): RamapResult<ShopOverView> {
        val requestedGeneration = generation
        val cached = cache[shopId]
        val result = ramenShopRepository.fetchShopOverview(shopId)
        if (result !is RamapResult.Success) return cached?.let { RamapResult.Success(it) } ?: result
        val notices = operatingNoticeRepository.fetchActiveShopOperatingNotices(shopId)
        val fetched =
            if (notices is RamapResult.Success) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.of(SEOUL_TIME_ZONE))
                result.data.copy(
                    operatingNotice =
                        notices.data
                            .filter { it.isRelevantForBusinessHoursAt(now) }
                            .sortedWith(compareByDescending<OperatingNotice> { it.businessDateAt(now) != null }.thenByDescending { it.updatedAt.orEmpty() }.thenBy { it.startDate }.thenBy { it.id })
                            .firstOrNull(),
                    operatingNotices = notices.data,
                )
            } else {
                result.data
            }
        val current = cache[shopId]
        val updated = current?.copy(event = fetched.event, operatingNotice = fetched.operatingNotice, operatingNotices = fetched.operatingNotices) ?: fetched
        if (requestedGeneration == generation) cache[shopId] = updated
        return RamapResult.Success(updated)
    }

    override fun findCached(shopId: String): ShopOverviewCacheLookup = cache[shopId]?.let(ShopOverviewCacheLookup::Hit) ?: ShopOverviewCacheLookup.Miss

    override fun clearCache() {
        generation++
        cache.clear()
    }

    override fun updateCachedLikeCount(
        shopId: String,
        enabled: Boolean,
    ) {
        cache[shopId]?.let { cache[shopId] = it.updateBookmarkCount(enabled) }
    }

    companion object {
        private const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
