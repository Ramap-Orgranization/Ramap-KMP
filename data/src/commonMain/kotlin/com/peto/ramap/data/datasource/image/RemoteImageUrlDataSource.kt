package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.network.client.media.MediaStorageClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal class RemoteImageUrlDataSource(
    private val client: SupabaseClient,
    private val media: MediaStorageClient,
) : ImageUrlDataSource {
    private val permits = Semaphore(MAX_CONCURRENT_BATCHES)

    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
        if (images.isEmpty()) return emptyMap()
        val userId = client.auth.currentUserOrNull()?.id
        val unique = images.distinct()
        val batches =
            unique
                .filter(::isSupported)
                .groupBy { it.bucket }
                .values
                .flatMap { it.chunked(MAX_IMAGES_PER_REQUEST) }
        val results = coroutineScope { batches.map { batch -> async { fetchBatch(batch) } }.awaitAll() }
        check(client.auth.currentUserOrNull()?.id == userId) { ERROR_SESSION_CHANGED }
        val resolved = results.fold(emptyMap<SignedImageRequest, SignedImageUrl?>()) { combined, batch -> combined + batch }
        return unique.associateWith { resolved[it] }
    }

    private fun isSupported(image: SignedImageRequest): Boolean = image.isSupported

    private suspend fun fetchBatch(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> =
        permits.withPermit {
            try {
                val responses = media.fetchUrls(images.first().bucket, images.map { it.path }).groupBy { it.path }
                images.associateWith { image ->
                    responses[image.path]?.singleOrNull()?.let { SignedImageUrl(it.url, it.expiresAt) }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                images.associateWith { null }
            }
        }

    private companion object {
        const val MAX_IMAGES_PER_REQUEST = 64
        const val MAX_CONCURRENT_BATCHES = 2
        const val ERROR_SESSION_CHANGED = "Image session changed"
    }
}
