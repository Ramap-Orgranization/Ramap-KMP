package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.data.model.SignedImageUrlsResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.FunctionRegion
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

internal class LegacyImageUrlBenchmarkDataSource(
    private val client: SupabaseClient,
) : ImageUrlDataSource {
    private val json = Json { ignoreUnknownKeys = true }
    private val permits = Semaphore(MAX_CONCURRENT_BATCHES)

    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
        if (images.isEmpty()) return emptyMap()
        val userId = client.auth.currentUserOrNull()?.id
        val results =
            coroutineScope {
                images
                    .distinct()
                    .chunked(MAX_IMAGES_PER_REQUEST)
                    .map { batch -> async { fetchBatch(batch) } }
                    .awaitAll()
                    .fold(emptyMap<SignedImageRequest, SignedImageUrl?>()) { combined, batch -> combined + batch }
            }
        check(client.auth.currentUserOrNull()?.id == userId) { ERROR_SESSION_CHANGED }
        return results
    }

    private suspend fun fetchBatch(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> =
        permits.withPermit {
            try {
                val response =
                    client.functions.invoke(
                        function = FUNCTION_IMAGE_SIGNED_URLS,
                        body = buildJsonObject { put(PARAMETER_IMAGES, json.encodeToJsonElement(images)) },
                        // Storage와 Redis가 있는 시드니에서 실행해 서버 간 왕복 시간을 줄인다.
                        region = FunctionRegion.AP_SOUTHEAST_2,
                        headers = Headers.build { append(HttpHeaders.ContentType, ContentType.Application.Json.toString()) },
                    )
                val signedImages = json.decodeFromString<SignedImageUrlsResponse>(response.bodyAsText()).images
                val urls = signedImages.associate { SignedImageRequest(it.bucket, it.path) to it.toSignedImageUrl() }
                images.associateWith { urls[it] }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                images.associateWith { null }
            }
        }

    private companion object {
        const val FUNCTION_IMAGE_SIGNED_URLS = "image-signed-urls"
        const val PARAMETER_IMAGES = "images"
        const val MAX_IMAGES_PER_REQUEST = 64
        const val MAX_CONCURRENT_BATCHES = 2
        const val ERROR_SESSION_CHANGED = "Image session changed"
    }
}
