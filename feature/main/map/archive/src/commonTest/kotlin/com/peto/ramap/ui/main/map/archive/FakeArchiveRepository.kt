package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.repository.ArchiveRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

internal class FakeArchiveRepository(
    var visits: ArchiveShops,
) : ArchiveRepository {
    var requests = 0
    var photoRequests = mutableListOf<String>()
    val photoBatches = mutableListOf<Set<String>>()
    var pending: CompletableDeferred<RamapResult<ArchiveShops>>? = null
    var pendingPhoto: CompletableDeferred<RamapResult<MyReviewPhoto>>? = null

    override suspend fun fetchArchiveShops(): RamapResult<ArchiveShops> {
        requests += 1
        val deferred = pending
        pending = null
        return deferred?.let { withContext(NonCancellable) { it.await() } } ?: RamapResult.Success(visits)
    }

    override suspend fun fetchShops(shopIds: Set<String>) = RamapResult.Success(visits.shops.filterByShopIds(shopIds))

    override suspend fun resolvePhotos(references: Set<String>): RamapResult<Map<String, MyReviewPhoto>> {
        photoRequests += references
        photoBatches += references
        val deferred = pendingPhoto
        pendingPhoto = null
        if (deferred != null) {
            return when (val result = withContext(NonCancellable) { deferred.await() }) {
                is RamapResult.Success -> RamapResult.Success(mapOf(references.first() to result.data))
                is RamapResult.Error -> result
            }
        }
        return RamapResult.Success(references.associateWith { MyReviewPhoto("https://example.com/$it", it, Clock.System.now() + 5.minutes) })
    }
}
