package com.peto.ramap.data.store

import com.peto.ramap.data.datasource.announcement.AnnouncementReadDataSource
import kotlinx.coroutines.CompletableDeferred

internal class AnnouncementReadDataSourceFake : AnnouncementReadDataSource {
    val reads = mutableMapOf<String, Set<String>>()
    var failWrite = false
    var fetchPending: CompletableDeferred<Set<String>>? = null
    var writePending: CompletableDeferred<Unit>? = null

    override suspend fun fetchReadIds(accountId: String): Set<String> = fetchPending?.await() ?: reads[accountId].orEmpty()

    override suspend fun markRead(
        announcementId: String,
        accountId: String,
    ) {
        check(!failWrite) { "서버 저장 실패" }
        writePending?.await()
        reads[accountId] = reads[accountId].orEmpty() + announcementId
    }
}
