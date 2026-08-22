package com.peto.ramap.data.datasource.report

import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission

internal interface ShopReportDataSource {
    suspend fun submitShopInformationReport(report: ShopInformationReportRequest)

    suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReportRequest)

    suspend fun submitNewsReport(report: NewsReport): NewsReportSubmission
}
