package com.peto.ramap.data.datasource.notice

import com.peto.ramap.data.model.OperatingNoticeResponse

internal interface OperatingNoticeDataSource {
    suspend fun fetchApprovedOperatingNotices(): List<OperatingNoticeResponse>
}
