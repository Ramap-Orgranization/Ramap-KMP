package com.peto.ramap.domain.store

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.announcement.AnnouncementReadState
import kotlinx.coroutines.flow.Flow

interface AnnouncementReadStore {
    val state: Flow<AnnouncementReadState>

    suspend fun refresh(): RamapResult<Unit>

    suspend fun markRead(
        announcementId: String,
        accountId: String?,
    ): RamapResult<Unit>
}
