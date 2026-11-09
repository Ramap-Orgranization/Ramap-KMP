package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.announcement.AnnouncementReadState
import com.peto.ramap.domain.store.AnnouncementReadStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeAnnouncementReadStore : AnnouncementReadStore {
    override val state = MutableStateFlow(AnnouncementReadState())
    var markReadResult: RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun refresh(): RamapResult<Unit> = RamapResult.Success(Unit)

    override suspend fun markRead(
        announcementId: String,
        accountId: String?,
    ): RamapResult<Unit> {
        if (markReadResult is RamapResult.Success) {
            state.update { it.copy(accountId = accountId, readIds = it.readIds + announcementId) }
        }
        return markReadResult
    }

    suspend fun markRead(announcementId: String) = markRead(announcementId, state.value.accountId)
}
