package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultOperatingNoticeRepository(
    private val operatingNoticeDataSource: OperatingNoticeDataSource,
    private val ramenShopDataSource: RamenShopDataSource,
) : OperatingNoticeRepository {
    override suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>> =
        invokeRequest {
            val now = Clock.System.now().toLocalDateTime(TimeZone.of(SEOUL_TIME_ZONE))
            val responses = operatingNoticeDataSource.fetchApprovedOperatingNotices(now.date)
            val shops =
                ramenShopDataSource
                    .fetchRamenShopsByIds(responses.mapTo(mutableSetOf()) { it.shopId })
                    .associateBy { it.id }
            responses
                .mapNotNull { response ->
                    val shop = shops[response.shopId]?.toDomain() ?: return@mapNotNull null
                    response.toDomain(shop)
                }.filter { it.isCurrentOrScheduledAt(now) }
        }

    override suspend fun fetchActiveShopOperatingNotices(shopId: String): RamapResult<List<OperatingNotice>> =
        invokeRequest {
            val today = today()
            val responses = operatingNoticeDataSource.fetchApprovedShopOperatingNotices(shopId, today.minus(1, DateTimeUnit.DAY))
            if (responses.isEmpty()) return@invokeRequest emptyList()

            val shop =
                ramenShopDataSource
                    .fetchRamenShopsByIds(setOf(shopId))
                    .firstOrNull()
                    ?.toDomain() ?: return@invokeRequest emptyList()

            responses.map { it.toDomain(shop) }
        }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.of(SEOUL_TIME_ZONE))

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
