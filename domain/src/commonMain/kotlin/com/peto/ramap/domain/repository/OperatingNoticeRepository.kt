package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed

interface OperatingNoticeRepository {
    suspend fun fetchOperatingNoticeFeed(): RamapResult<OperatingNoticeFeed>

    suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>>

    suspend fun fetchActiveShopOperatingNotices(shopId: String): RamapResult<List<OperatingNotice>>
}
