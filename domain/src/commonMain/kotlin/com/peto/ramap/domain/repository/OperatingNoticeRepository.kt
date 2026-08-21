package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notice.OperatingNotice

interface OperatingNoticeRepository {
    suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>>

    suspend fun fetchActiveShopOperatingNotice(shopId: String): RamapResult<OperatingNotice?>
}
