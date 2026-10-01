package com.peto.ramap.data.datasource.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class ReviewImageSigner(
    private val sign: suspend (String) -> String,
) {
    private val permits = Semaphore(MAX_CONCURRENT_REQUESTS)

    suspend fun signImages(paths: List<String>): List<String> =
        coroutineScope {
            paths.map { path -> async { signImage(path) } }.awaitAll().filterNotNull()
        }

    suspend fun signImagesOrNull(paths: List<String>): List<String?> =
        coroutineScope {
            paths.map { path -> async { signImage(path) } }.awaitAll()
        }

    private suspend fun signImage(path: String): String? =
        permits.withPermit {
            try {
                sign(path)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
        }

    private companion object {
        const val MAX_CONCURRENT_REQUESTS = 4
    }
}
