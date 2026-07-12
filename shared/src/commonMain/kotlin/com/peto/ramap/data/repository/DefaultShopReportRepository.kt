package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.network.execute.invokeRequest

class DefaultShopReportRepository(
    private val dataSource: ShopReportDataSource,
) : ShopReportRepository {
    override suspend fun submit(report: ShopInformationReport): RamapResult<Unit> =
        invokeRequest { dataSource.insert(ShopInformationReportRequest.from(report)) }

    override suspend fun submit(report: UnregisteredPlaceReport): RamapResult<Unit> =
        invokeRequest { dataSource.insert(UnregisteredPlaceReportRequest.from(report)) }
}
