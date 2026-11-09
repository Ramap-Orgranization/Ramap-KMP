package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.announcement.AnnouncementDataSource
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.domain.repository.AnnouncementRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultAnnouncementRepository(
    private val dataSource: AnnouncementDataSource,
) : AnnouncementRepository {
    override suspend fun fetchAnnouncements(): RamapResult<List<Announcement>> = invokeRequest { dataSource.fetchAnnouncements().map { it.toDomain() } }
}
