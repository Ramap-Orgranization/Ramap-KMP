package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.data.model.StorageSignedImageResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(SupabaseInternal::class)
internal class RemoteImageUrlDataSource(
    private val client: SupabaseClient,
) : ImageUrlDataSource {
    private val json = Json { ignoreUnknownKeys = true }
    private val permits = Semaphore(MAX_CONCURRENT_BATCHES)

    // Use the SDK's current JWT/anon resolution, with a nullable DTO for per-object failures.
    private val api by lazy { client.authenticatedSupabaseApi(client.storage) }

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

    private fun isSupported(image: SignedImageRequest): Boolean = image.bucket in BUCKETS && IMAGE_PATH.matches(image.path)

    private suspend fun fetchBatch(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> =
        permits.withPermit {
            try {
                val expiresAt = Instant.fromEpochSeconds(Clock.System.now().epochSeconds) + SIGNED_URL_LIFETIME
                val response =
                    api.postJson(
                        url = "object/sign/${images.first().bucket}",
                        body =
                            buildJsonObject {
                                putJsonArray("paths") { images.forEach { add(it.path) } }
                                put("expiresIn", SIGNED_URL_LIFETIME.inWholeSeconds)
                            },
                    )
                val responses = json.decodeFromString<List<StorageSignedImageResponse>>(response.bodyAsText()).groupBy { it.path }
                images.associateWith { image -> resolve(image, responses[image.path]?.singleOrNull(), expiresAt) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                images.associateWith { null }
            }
        }

    private fun resolve(
        image: SignedImageRequest,
        response: StorageSignedImageResponse?,
        expiresAt: Instant,
    ): SignedImageUrl? {
        if (response == null || response.error != null) return null
        val relativeUrl = response.signedUrl ?: return null
        val objectPath = "object/sign/${image.bucket}/${image.path}"
        if (!relativeUrl.startsWith("/$objectPath?")) return null
        val url = runCatching { Url(client.storage.resolveUrl(relativeUrl.removePrefix("/"))) }.getOrNull() ?: return null
        if (url.encodedPath != Url(client.storage.resolveUrl(objectPath)).encodedPath || url.fragment.isNotEmpty() || url.parameters["token"].isNullOrBlank()) return null
        if (expiresAt <= Clock.System.now() + REFRESH_MARGIN) return null
        return SignedImageUrl(url.toString(), expiresAt)
    }

    private companion object {
        const val MAX_IMAGES_PER_REQUEST = 64
        const val MAX_CONCURRENT_BATCHES = 2
        const val ERROR_SESSION_CHANGED = "Image session changed"
        val SIGNED_URL_LIFETIME = 5.minutes
        val REFRESH_MARGIN = 1.minutes
        val BUCKETS = setOf("profile-avatars", "shop-review-images")
        val IMAGE_PATH = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)", RegexOption.IGNORE_CASE)
    }
}
