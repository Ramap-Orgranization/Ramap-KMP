package com.peto.ramap.fake

import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission

internal class FakeShopReportDataSource : ShopReportDataSource {
    var insertedReport: ShopInformationReportRequest? = null
        private set
    var insertedPlaceReport: UnregisteredPlaceReportRequest? = null
        private set
    var insertedNewsReport: NewsReport? = null
        private set

    override suspend fun submitShopInformationReport(report: ShopInformationReportRequest) {
        insertedReport = report
    }

    override suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReportRequest) {
        insertedPlaceReport = report
    }

    override suspend fun submitNewsReport(report: NewsReport): NewsReportSubmission {
        insertedNewsReport = report
        return NewsReportSubmission.SUBMITTED
    }
}
