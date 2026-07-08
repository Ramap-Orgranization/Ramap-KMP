package com.peto.ramap.fake

import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.model.ShopInformationReportRequest

class FakeShopReportDataSource : ShopReportDataSource {
    var insertedReport: ShopInformationReportRequest? = null
        private set

    override suspend fun insert(report: ShopInformationReportRequest) {
        insertedReport = report
    }
}
