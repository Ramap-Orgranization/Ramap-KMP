package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.announcement.Announcement

interface AnnouncementRepository {
    suspend fun fetchAnnouncements(): RamapResult<List<Announcement>>
}
