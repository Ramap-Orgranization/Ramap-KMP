package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.model.EditableReviewResponse
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.ReviewLikeResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.model.ShopReviewsPageResponse
import com.peto.ramap.domain.model.review.ReviewImage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
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

    override suspend fun fetchMyReviews(
        offset: Long,
        visibility: String,
    ): MyReviewsPageResponse {
        val page =
            client.postgrest
                .rpc(
                    RPC_FETCH_MY_REVIEWS,
                    buildJsonObject {
                        put(PARAM_OFFSET, offset)
                        put(PARAM_VISIBILITY, visibility)
                    },
                ).decodeAs<MyReviewsPageResponse>()
        return page.copy(
            reviews =
                coroutineScope {
                    page.reviews.map { review -> async { signMyReview(review) } }.awaitAll()
                },
        )
    }

    override suspend fun fetchMyReview(reviewId: String): MyReviewResponse? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_MY_REVIEW,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeAs<MyReviewResponse?>() ?: return null
        return signMyReview(review)
    }

    override suspend fun fetchShopReviews(
        shopId: String,
        offset: Long,
    ): List<ReviewResponse> =
        fetchReviews(
            RPC_FETCH_SHOP_REVIEWS,
            buildJsonObject {
                put(PARAM_SHOP_ID, shopId)
                put(PARAM_OFFSET, offset)
            },
        )

    override suspend fun fetchShopReviewsPage(
        shopId: String,
        offset: Long,
    ): ShopReviewsPageResponse {
        val page =
            client.postgrest
                .rpc(
                    RPC_FETCH_SHOP_REVIEWS_PAGE,
                    buildJsonObject {
                        put(PARAM_SHOP_ID, shopId)
                        put(PARAM_OFFSET, offset)
                    },
                ).decodeAs<ShopReviewsPageResponse>()
        return page.copy(reviews = signReviews(page.reviews))
    }

    override suspend fun fetchBlockedShopReviewOnce(reviewId: String): ReviewResponse? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_BLOCKED_SHOP_REVIEW_ONCE,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeAs<ReviewResponse?>() ?: return null
        return signReviews(listOf(review)).single()
    }

    override suspend fun fetchProfileReviews(
        userId: String?,
        offset: Long,
    ): List<ReviewResponse> =
        fetchReviews(
            RPC_FETCH_PROFILE_REVIEWS,
            buildJsonObject {
                put(PARAM_USER_ID, userId)
                put(PARAM_OFFSET, offset)
            },
        )

    override suspend fun submitReview(
        review: ShopReviewRequest,
        images: List<ReviewImage>,
    ) {
        val paths = uploadImages(images)
        try {
            client.postgrest.rpc(
                RPC_SUBMIT_SHOP_REVIEW,
                buildJsonObject {
                    put(PARAM_SHOP_ID, review.shopId)
                    put(PARAM_BODY, review.body)
                    put(PARAM_IMAGE_PATHS, JsonArray(paths.map(::JsonPrimitive)))
                    put(PARAM_IS_PUBLIC, review.isPublic)
                },
            )
        } catch (error: Throwable) {
            cleanup(paths)
            throw error
        }
    }

    override suspend fun fetchEditableReview(reviewId: String): Pair<EditableReviewResponse, List<String?>>? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_EDITABLE_REVIEW,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeList<EditableReviewResponse>()
                .singleOrNull() ?: return null
        val urls =
            review.imagePaths.map { path ->
                try {
                    client.storage.from(BUCKET).createSignedUrl(path, SIGNED_URL_LIFETIME)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    null
                }
            }
        return review to urls
    }

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ) {
        val oldPaths =
            fetchEditableReview(reviewId)?.first?.imagePaths
                ?: error("Review is unavailable")
        val uploadedPaths = uploadImages(newImages)
        try {
            client.postgrest.rpc(
                RPC_UPDATE_REVIEW,
                buildJsonObject {
                    put(PARAM_REVIEW_ID, reviewId)
                    put(PARAM_BODY, body)
                    put(PARAM_IMAGE_PATHS, JsonArray((retainedImagePaths + uploadedPaths).map(::JsonPrimitive)))
                    put(PARAM_IS_PUBLIC, isPublic)
                },
            )
            cleanup(oldPaths.filterNot { it in retainedImagePaths })
        } catch (error: Throwable) {
            cleanup(uploadedPaths)
            throw error
        }
    }

    override suspend fun deleteReview(reviewId: String) {
        val oldPaths =
            fetchEditableReview(reviewId)?.first?.imagePaths
                ?: error("Review is unavailable")
        client.postgrest.rpc(
            RPC_DELETE_REVIEW,
            buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
        )
        cleanup(oldPaths)
    }

    override suspend fun setReviewLike(
        reviewId: String,
        liked: Boolean,
    ): ReviewLikeResponse =
        client.postgrest
            .rpc(
                RPC_SET_REVIEW_LIKE,
                buildJsonObject {
                    put(PARAM_REVIEW_ID, reviewId)
                    put(PARAM_LIKED, liked)
                },
            ).decodeList<ReviewLikeResponse>()
            .single()

    private suspend fun fetchReviews(
        rpc: String,
        parameters: kotlinx.serialization.json.JsonObject,
    ): List<ReviewResponse> = signReviews(client.postgrest.rpc(rpc, parameters).decodeList<ReviewResponse>())

    private suspend fun signReviews(reviews: List<ReviewResponse>): List<ReviewResponse> =
        coroutineScope {
            reviews
                .map { review ->
                    async {
                        review.copy(
                            imageUrls = imageSigner.signImages(review.imagePaths),
                            avatarUrl = signedAvatar(review.avatarPath),
                        )
                    }
                }.awaitAll()
        }

    private suspend fun signedAvatar(path: String?): String? {
        if (path == null) return null
        return try {
            client.storage.from(AVATAR_BUCKET).createSignedUrl(path, SIGNED_URL_LIFETIME)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun signMyReview(review: MyReviewResponse): MyReviewResponse =
        review.copy(
            imageUrls = imageSigner.signImagesOrNull(review.imagePaths),
            authorAvatarUrl = signedAvatar(review.authorAvatarPath),
        )

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
        const val RPC_FETCH_MY_REVIEWS = "fetch_my_reviews"
        const val RPC_FETCH_MY_REVIEW = "fetch_my_review"
        const val RPC_FETCH_SHOP_REVIEWS = "fetch_shop_reviews"
        const val RPC_FETCH_SHOP_REVIEWS_PAGE = "fetch_shop_reviews_page"
        const val RPC_FETCH_BLOCKED_SHOP_REVIEW_ONCE = "fetch_blocked_shop_review_once"
        const val RPC_FETCH_PROFILE_REVIEWS = "fetch_profile_reviews"
        const val RPC_SUBMIT_SHOP_REVIEW = "submit_shop_review"
        const val RPC_FETCH_EDITABLE_REVIEW = "fetch_editable_shop_review"
        const val RPC_UPDATE_REVIEW = "update_shop_review"
        const val RPC_DELETE_REVIEW = "delete_shop_review"
        const val RPC_SET_REVIEW_LIKE = "set_review_like"

        const val PARAM_SHOP_ID = "p_shop_id"
        const val PARAM_USER_ID = "p_user_id"
        const val PARAM_OFFSET = "p_offset"
        const val PARAM_VISIBILITY = "p_visibility"
        const val PARAM_BODY = "p_body"
        const val PARAM_IMAGE_PATHS = "p_image_paths"
        const val PARAM_IS_PUBLIC = "p_is_public"
        const val PARAM_REVIEW_ID = "p_review_id"
        const val PARAM_LIKED = "p_liked"

        const val BUCKET = "shop-review-images"
        const val AVATAR_BUCKET = "profile-avatars"
        val SIGNED_URL_LIFETIME = 5.minutes
    }
}
