package com.peto.ramap.image

import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

internal class ImageCacheMigrationInterceptor(
    private val diskCache: () -> DiskCache?,
) : Interceptor {
    private val mutex = Mutex()
    private var migrated = false

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val ready =
            mutex.withLock {
                if (migrated) return@withLock true
                withContext(Dispatchers.IO) {
                    val cache = diskCache() ?: return@withContext true
                    val marker = cache.directory.parent?.resolve(MARKER) ?: return@withContext false
                    try {
                        if (!cache.fileSystem.exists(marker)) {
                            cache.clear()
                            cache.fileSystem.write(marker) { writeUtf8("2") }
                        }
                        migrated = true
                        true
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        false
                    }
                }
            }
        return if (ready) {
            chain.proceed()
        } else {
            chain
                .withRequest(
                    chain.request
                        .newBuilder()
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                ).proceed()
        }
    }

    private companion object {
        const val MARKER = "ramap-image-cache-policy-v2"
    }
}
