package com.peto.ramap.data.datasource.notice

import com.peto.ramap.data.model.OperatingNoticeFeedResponse
import com.peto.ramap.data.model.OperatingNoticeResponse
import kotlinx.datetime.LocalDate

internal interface OperatingNoticeDataSource {
    suspend fun fetchOperatingNoticeFeed(today: LocalDate): OperatingNoticeFeedResponse

    suspend fun fetchApprovedOperatingNotices(today: LocalDate): List<OperatingNoticeResponse>

    suspend fun fetchApprovedShopOperatingNotices(
        shopId: String,
        earliestDate: LocalDate,
    ): List<OperatingNoticeResponse>
}
