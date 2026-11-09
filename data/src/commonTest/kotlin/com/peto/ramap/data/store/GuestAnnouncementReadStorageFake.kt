package com.peto.ramap.data.store

import com.peto.ramap.platform.storage.AnnouncementReadStorage
import kotlinx.coroutines.flow.MutableStateFlow

internal class GuestAnnouncementReadStorageFake : AnnouncementReadStorage {
    override val readIds = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun markRead(announcementId: String) {
        readIds.value += announcementId
    }
}
