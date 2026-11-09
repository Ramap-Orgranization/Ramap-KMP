package com.peto.ramap.data.datasource.announcement

import com.peto.ramap.data.model.AnnouncementResponse

internal interface AnnouncementDataSource {
    suspend fun fetchAnnouncements(): List<AnnouncementResponse>
}
