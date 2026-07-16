package com.peto.ramap.fake

import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest

class FakeShopReportDataSource : ShopReportDataSource {
    var insertedReport: ShopInformationReportRequest? = null
        private set
    var insertedPlaceReport: UnregisteredPlaceReportRequest? = null
        private set

    override suspend fun submitShopInformationReport(report: ShopInformationReportRequest) {
        insertedReport = report
    }

    override suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReportRequest) {
        insertedPlaceReport = report
    }
}
