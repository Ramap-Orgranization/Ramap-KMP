package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import kotlinx.coroutines.delay

class FakeOperatingNoticeRepository(
    var notices: List<OperatingNotice> = emptyList(),
    var activeNotice: OperatingNotice? = null,
    var error: RamapError? = null,
    var activeNoticeError: RamapError? = null,
    var delayMillis: Long = 0,
) : OperatingNoticeRepository {
    var fetchCount = 0
        private set

    private val _requestedActiveNoticeShopIds = mutableListOf<String>()
    val requestedActiveNoticeShopIds: List<String> = _requestedActiveNoticeShopIds

    override suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>> {
        fetchCount++
        if (delayMillis > 0) delay(delayMillis)
        return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(notices)
    }

    override suspend fun fetchActiveShopOperatingNotice(shopId: String): RamapResult<OperatingNotice?> {
        _requestedActiveNoticeShopIds.add(shopId)
        return activeNoticeError?.let { RamapResult.Error(it) } ?: RamapResult.Success(activeNotice)
    }
}
