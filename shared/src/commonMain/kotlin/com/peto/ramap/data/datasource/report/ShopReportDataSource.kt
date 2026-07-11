package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest

interface ShopReportDataSource {
    suspend fun insert(report: ShopInformationReportRequest)

    suspend fun insert(report: UnregisteredPlaceReportRequest)
}
