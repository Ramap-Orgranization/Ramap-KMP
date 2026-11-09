package com.peto.ramap.platform.storage

import kotlinx.coroutines.flow.Flow

interface AnnouncementReadStorage {
    val readIds: Flow<Set<String>>

    suspend fun markRead(announcementId: String)
}
