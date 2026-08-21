package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.domain.model.operatingnotice.OperatingNotice
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultOperatingNoticeRepository(
    private val operatingNoticeDataSource: OperatingNoticeDataSource,
    private val ramenShopDataSource: RamenShopDataSource,
) : OperatingNoticeRepository {
    override suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>> =
        invokeRequest {
            val responses = operatingNoticeDataSource.fetchApprovedOperatingNotices()
            val today = today()
            val shops =
                ramenShopDataSource
                    .fetchRamenShopsByIds(responses.mapTo(mutableSetOf()) { it.shopId })
                    .associateBy { it.id }
            responses
                .mapNotNull { response ->
                    val shop = shops[response.shopId]?.toDomain() ?: return@mapNotNull null
                    response.toDomain(shop)
                }.filter { notice -> notice.statusOn(today) != null }
        }

    private fun today(): LocalDate = Clock.System.todayIn(TimeZone.of(SEOUL_TIME_ZONE))

    private companion object {
        const val SEOUL_TIME_ZONE = "Asia/Seoul"
    }
}
