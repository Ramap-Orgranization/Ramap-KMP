package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.report.NewsReport
import com.peto.ramap.domain.model.report.NewsReportSubmission
import com.peto.ramap.domain.model.report.ShopInformationReport
import com.peto.ramap.domain.model.report.UnregisteredPlaceReport

interface ShopReportRepository {
    suspend fun submitShopInformationReport(report: ShopInformationReport): RamapResult<Unit>

    suspend fun submitUnregisteredPlaceReport(report: UnregisteredPlaceReport): RamapResult<Unit>

    suspend fun submitNewsReport(report: NewsReport): RamapResult<NewsReportSubmission>
}
