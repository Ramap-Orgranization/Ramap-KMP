package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.update.AppUpdatePolicyDataSource
import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.domain.repository.AppUpdateRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultAppUpdateRepository(
    private val dataSource: AppUpdatePolicyDataSource,
) : AppUpdateRepository {
    override suspend fun fetchAppUpdatePolicy(platform: String): RamapResult<AppUpdatePolicy?> =
        invokeRequest { dataSource.fetchAppUpdatePolicy(platform)?.toDomain() }
}
