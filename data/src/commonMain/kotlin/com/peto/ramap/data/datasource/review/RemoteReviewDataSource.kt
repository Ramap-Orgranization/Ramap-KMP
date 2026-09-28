package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.model.ShopReviewResponse
import com.peto.ramap.domain.model.review.ReviewImage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

internal class RemoteReviewDataSource(
    private val client: SupabaseClient,
) : ReviewDataSource {
    private val imageSigner = ReviewImageSigner { path -> client.storage.from(BUCKET).createSignedUrl(path, SIGNED_URL_LIFETIME) }

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): List<ShopReviewResponse> =
        fetchReviews(
            "fetch_shop_reviews",
            buildJsonObject {
                put("p_shop_id", shopId)
                put("p_offset", offset)
            },
        )

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): List<ShopReviewResponse> =
        fetchReviews(
            "fetch_profile_reviews",
            buildJsonObject {
                put("p_user_id", userId)
                put("p_offset", offset)
            },
        )

    override suspend fun submitReview(
        review: ShopReviewRequest,
        images: List<ReviewImage>,
    ) {
        val paths = uploadImages(images)
        try {
            client.postgrest.rpc(
                "submit_shop_review",
                buildJsonObject {
                    put("p_shop_id", review.shopId)
                    put("p_body", review.body)
                    put("p_image_paths", JsonArray(paths.map(::JsonPrimitive)))
                    put("p_is_public", review.isPublic)
                },
            )
        } catch (error: Throwable) {
            cleanup(paths)
            throw error
        }
    }

    private suspend fun fetchReviews(
        rpc: String,
        parameters: kotlinx.serialization.json.JsonObject,
    ): List<ShopReviewResponse> =
        coroutineScope {
            client.postgrest
                .rpc(rpc, parameters)
                .decodeList<ShopReviewResponse>()
                .map { review ->
                    async { review.copy(imageUrls = imageSigner.signImages(review.imagePaths)) }
                }.awaitAll()
        }

    private suspend fun uploadImages(images: List<ReviewImage>): List<String> {
        val userId = requireNotNull(client.auth.currentUserOrNull()?.id) { "Missing authenticated user" }
        if (images.isEmpty()) return emptyList()
        val paths = mutableListOf<String>()
        try {
            images.forEach { image ->
                val path = "$userId/${Uuid.random()}.${image.fileExtension}"
                paths += path
                client.storage.from(BUCKET).upload(path, image.bytes) { contentType = ContentType.parse(image.mimeType) }
            }
            return paths
        } catch (error: Throwable) {
            cleanup(paths)
            throw error
        }
    }

    private suspend fun cleanup(paths: List<String>) =
        withContext(NonCancellable) {
            paths.forEach { path -> runCatching { client.storage.from(BUCKET).delete(path) } }
        }

    private companion object {
        const val BUCKET = "shop-review-images"
        val SIGNED_URL_LIFETIME = 5.minutes
    }
}
