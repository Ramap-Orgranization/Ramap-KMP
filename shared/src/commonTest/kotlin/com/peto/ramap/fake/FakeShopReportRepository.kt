package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.ShopReportRepository
import kotlinx.coroutines.delay

class FakeShopReportRepository(
    private val error: Throwable? = null,
    private val delayMillis: Long = 0,
) : ShopReportRepository {
    val reports = mutableListOf<ShopInformationReport>()
    val placeReports = mutableListOf<UnregisteredPlaceReport>()

    override suspend fun submit(report: ShopInformationReport): RamapResult<Unit> {
        delay(delayMillis)
        error?.let { return RamapResult.Error(RamapError.Unknown(it)) }
        reports += report
        return RamapResult.Success(Unit)
    }

    override suspend fun submit(report: UnregisteredPlaceReport): RamapResult<Unit> {
        delay(delayMillis)
        error?.let { return RamapResult.Error(RamapError.Unknown(it)) }
        placeReports += report
        return RamapResult.Success(Unit)
    }
}
