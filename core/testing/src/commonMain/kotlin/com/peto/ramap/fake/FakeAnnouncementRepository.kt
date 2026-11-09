package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.domain.repository.AnnouncementRepository
import kotlinx.coroutines.CompletableDeferred

class FakeAnnouncementRepository(
    var result: RamapResult<List<Announcement>> = RamapResult.Success(emptyList()),
) : AnnouncementRepository {
    var pending: CompletableDeferred<RamapResult<List<Announcement>>>? = null

    override suspend fun fetchAnnouncements(): RamapResult<List<Announcement>> = pending?.await() ?: result
}
