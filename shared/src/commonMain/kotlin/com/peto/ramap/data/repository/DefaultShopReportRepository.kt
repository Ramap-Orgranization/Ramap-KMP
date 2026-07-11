package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.ShopReportRepository

class DefaultShopReportRepository(
    private val dataSource: ShopReportDataSource,
) : ShopReportRepository {
    override suspend fun submit(report: ShopInformationReport) {
        dataSource.insert(ShopInformationReportRequest.from(report))
    }

    override suspend fun submit(report: UnregisteredPlaceReport) {
        dataSource.insert(UnregisteredPlaceReportRequest.from(report))
    }
}
