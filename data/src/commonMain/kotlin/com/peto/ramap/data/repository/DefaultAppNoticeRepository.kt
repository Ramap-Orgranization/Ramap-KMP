package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.appnotice.AppNoticeDataSource
import com.peto.ramap.domain.model.appnotice.AppNotice
import com.peto.ramap.domain.repository.AppNoticeRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultAppNoticeRepository(
    private val dataSource: AppNoticeDataSource,
) : AppNoticeRepository {
    override suspend fun fetchActiveAppNotice(platform: String): RamapResult<AppNotice?> = invokeRequest { dataSource.fetchActiveAppNotice(platform)?.toDomain() }
}
