package com.peto.ramap.fake

import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.ShopReportRepository

class FakeShopReportRepository(
    private val error: Throwable? = null,
) : ShopReportRepository {
    val reports = mutableListOf<ShopInformationReport>()
    val placeReports = mutableListOf<UnregisteredPlaceReport>()

    override suspend fun submit(report: ShopInformationReport) {
        error?.let { throw it }
        reports += report
    }

    override suspend fun submit(report: UnregisteredPlaceReport) {
        error?.let { throw it }
        placeReports += report
    }
}
