package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.update.AppUpdatePolicy

interface AppUpdateRepository {
    suspend fun fetchAppUpdatePolicy(platform: String): RamapResult<AppUpdatePolicy?>
}
