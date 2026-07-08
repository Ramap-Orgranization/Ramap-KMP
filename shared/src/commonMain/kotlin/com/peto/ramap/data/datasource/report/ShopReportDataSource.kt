package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.ShopInformationReportRequest

interface ShopReportDataSource {
    suspend fun insert(report: ShopInformationReportRequest)
}
