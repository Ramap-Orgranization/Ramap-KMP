package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopReportRepository(
    private val dataSource: ShopReportDataSource,
) : ShopReportRepository {
    override suspend fun submitShopInformationReport(report: ShopInformationReport): RamapResult<Unit> =
        invokeRequest { dataSource.submitShopInformationReport(ShopInformationReportRequest.from(report)) }

    override suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReport): RamapResult<Unit> =
        invokeRequest { dataSource.submitUnregisteredPlaceReport(UnregisteredPlaceReportRequest.from(report)) }

    override suspend fun submitNewsReport(report: NewsReport): RamapResult<NewsReportSubmission> =
        invokeRequest { dataSource.submitNewsReport(report) }
}
