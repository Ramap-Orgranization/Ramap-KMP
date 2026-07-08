package com.peto.ramap.domain.repository

import com.peto.ramap.domain.model.ShopInformationReport

interface ShopReportRepository {
    suspend fun submit(report: ShopInformationReport)
}
