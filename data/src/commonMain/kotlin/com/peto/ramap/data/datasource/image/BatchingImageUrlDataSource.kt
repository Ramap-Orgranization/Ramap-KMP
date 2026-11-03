package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

internal class BatchingImageUrlDataSource(
    private val dataSource: ImageUrlDataSource,
    private val accessPolicy: ImageUrlAccessPolicy,
    private val scope: CoroutineScope,
) : ImageUrlDataSource {
    private val mutex = Mutex()
    private val requests = mutableMapOf<Pair<ImageUrlAccessSnapshot, SignedImageRequest>, PendingImageUrlRequest>()
    private val queued = mutableSetOf<PendingImageUrlRequest>()
    private var scheduled: Job? = null

    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
        if (images.isEmpty()) return emptyMap()
        scope.coroutineContext.ensureActive()
        val access = accessPolicy.snapshot()
        val pending =
            mutex.withLock {
                images
                    .distinct()
                    .map { image ->
                        requests
                            .getOrPut(access to image) {
                                PendingImageUrlRequest(image, access, CompletableDeferred(scope.coroutineContext[Job])).also(queued::add)
                            }.also(PendingImageUrlRequest::retain)
                    }.also { scheduleBatch() }
            }
        try {
            val resolved = pending.associate { it.image to it.result.await().getOrThrow() }
            check(accessPolicy.invalidatedImages(access, images).isEmpty()) { ERROR_ACCESS_CHANGED }
            return resolved
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    for (request in pending) {
                        if (request.release()) {
                            discard(request)
                            queued.remove(request)
                            request.result.cancel()
                        }
                    }
                }
            }
        }
    }

    private fun scheduleBatch() {
        if (scheduled != null || queued.isEmpty()) return
        scheduled =
            scope.launch {
                delay(BATCH_WINDOW)
                val batch =
                    mutex.withLock {
                        scheduled = null
                        queued.toList().also { queued.clear() }
                    }
                batch.groupBy { it.access }.forEach { (access, requests) -> fetchBatch(access, requests) }
            }
    }

    private suspend fun fetchBatch(
        access: ImageUrlAccessSnapshot,
        batch: List<PendingImageUrlRequest>,
    ) {
        val active = batch.filterNot { it.result.isCancelled }
        try {
            val images = active.map { it.image }
            if (images.isEmpty()) return
            check(accessPolicy.invalidatedImages(access, images).isEmpty()) { ERROR_ACCESS_CHANGED }
            val resolved = dataSource.fetchUrls(images)
            check(accessPolicy.invalidatedImages(access, images).isEmpty()) { ERROR_ACCESS_CHANGED }
            active.forEach { it.result.complete(Result.success(resolved[it.image])) }
        } catch (exception: Exception) {
            active.forEach { it.result.complete(Result.failure(exception)) }
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { batch.forEach(::discard) }
            }
        }
    }

    private fun discard(request: PendingImageUrlRequest) {
        val key = request.access to request.image
        if (requests[key] === request) requests.remove(key)
    }

    private companion object {
        val BATCH_WINDOW = 20.milliseconds
        const val ERROR_ACCESS_CHANGED = "Image access changed"
    }
}
