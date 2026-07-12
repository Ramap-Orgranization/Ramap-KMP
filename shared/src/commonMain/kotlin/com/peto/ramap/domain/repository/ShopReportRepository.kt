package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport

interface ShopReportRepository {
    suspend fun submit(report: ShopInformationReport): RamapResult<Unit>

    suspend fun submit(report: UnregisteredPlaceReport): RamapResult<Unit>
}
