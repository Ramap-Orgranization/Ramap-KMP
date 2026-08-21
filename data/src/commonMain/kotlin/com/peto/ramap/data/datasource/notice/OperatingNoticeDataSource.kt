package com.peto.ramap.data.datasource.notice

import com.peto.ramap.data.model.OperatingNoticeResponse
import kotlinx.datetime.LocalDate

internal interface OperatingNoticeDataSource {
    suspend fun fetchApprovedOperatingNotices(today: LocalDate): List<OperatingNoticeResponse>

    suspend fun fetchApprovedShopOperatingNotices(
        shopId: String,
        today: LocalDate,
    ): List<OperatingNoticeResponse>
}
