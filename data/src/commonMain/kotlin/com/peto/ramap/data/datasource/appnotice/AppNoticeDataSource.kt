package com.peto.ramap.data.datasource.appnotice

import com.peto.ramap.data.model.AppNoticeResponse

internal interface AppNoticeDataSource {
    suspend fun fetchActiveAppNotice(platform: String): AppNoticeResponse?
}
