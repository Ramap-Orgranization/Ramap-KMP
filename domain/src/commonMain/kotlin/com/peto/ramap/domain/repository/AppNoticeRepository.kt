package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.appnotice.AppNotice

interface AppNoticeRepository {
    suspend fun fetchActiveAppNotice(platform: String): RamapResult<AppNotice?>
}
