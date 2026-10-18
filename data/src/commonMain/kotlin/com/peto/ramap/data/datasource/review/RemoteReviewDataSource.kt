package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.EditableReviewResponse
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.ProfileReviewsPageResponse
import com.peto.ramap.data.model.ReviewLikeResponse
import com.peto.ramap.data.model.ReviewMutationResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.model.ShopReviewsPageResponse
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.domain.model.review.MyReviewPhoto
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.store.ReviewChangeStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

internal class RemoteReviewDataSource(
    private val client: SupabaseClient,
    private val imageUrlDataSource: ImageUrlDataSource,
    private val changes: ReviewChangeStore,
) : ReviewDataSource {
    private val signing = Mutex()
    private val signedUrls = mutableMapOf<SignedImageRequest, SignedImageUrl>()
    private var signedUserId: String? = null
    private var signedEpoch = -1L
    private var signedRevision = 0L

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
        return page.copy(reviews = signMyReviews(page.reviews))
    }

    override suspend fun fetchMyReview(reviewId: String): MyReviewResponse? {
        val review =
            client.postgrest
                .rpc(
                    RPC_FETCH_MY_REVIEW,
                    buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
                ).decodeAs<MyReviewResponse?>() ?: return null
        return signMyReviews(listOf(review)).single()
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

    override suspend fun fetchProfileReviewsPage(
        userId: String,
        offset: Long,
    ): ProfileReviewsPageResponse {
        val page =
            client.postgrest
                .rpc(
                    "fetch_profile_reviews_page_v2",
                    buildJsonObject {
                        put(PARAM_USER_ID, userId)
                        put(PARAM_OFFSET, offset)
                    },
                ).decodeAs<ProfileReviewsPageResponse>()
        return page.copy(reviews = signReviews(page.reviews))
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
    ): ReviewMutationResponse {
        val paths = uploadImages(images)
        try {
            return client.postgrest
                .rpc(
                    RPC_SUBMIT_SHOP_REVIEW,
                    buildJsonObject {
                        put(PARAM_SHOP_ID, review.shopId)
                        put(PARAM_BODY, review.body)
                        put(PARAM_IMAGE_PATHS, JsonArray(paths.map(::JsonPrimitive)))
                        put(PARAM_IS_PUBLIC, review.isPublic)
                    },
                ).decodeAs<ReviewMutationResponse>()
        } catch (error: Throwable) {
            cleanupIfRejected(paths, error)
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
        val images = review.imagePaths.map { SignedImageRequest(bucket = BUCKET, path = it) }
        val urls = fetchImageUrls(images)
        return review to images.map { urls[it]?.url }
    }

    override suspend fun updateReview(
        reviewId: String,
        body: String,
        retainedImagePaths: List<String>,
        newImages: List<ReviewImage>,
        isPublic: Boolean,
    ): ReviewMutationResponse {
        val uploadedPaths = uploadImages(newImages)
        try {
            return client.postgrest
                .rpc(
                    RPC_UPDATE_REVIEW,
                    buildJsonObject {
                        put(PARAM_REVIEW_ID, reviewId)
                        put(PARAM_BODY, body)
                        put(PARAM_IMAGE_PATHS, JsonArray((retainedImagePaths + uploadedPaths).map(::JsonPrimitive)))
                        put(PARAM_IS_PUBLIC, isPublic)
                    },
                ).decodeAs<ReviewMutationResponse>()
        } catch (error: Throwable) {
            cleanupIfRejected(uploadedPaths, error)
            throw error
        }
    }

    override suspend fun deleteReview(reviewId: String): ReviewMutationResponse =
        client.postgrest
            .rpc(
                RPC_DELETE_REVIEW,
                buildJsonObject { put(PARAM_REVIEW_ID, reviewId) },
            ).decodeAs<ReviewMutationResponse>()

    override suspend fun hydrateMutation(response: ReviewMutationResponse): ReviewMutationResponse {
        val retainedPaths = response.after?.imagePaths.orEmpty()
        cleanup(
            response.before
                ?.imagePaths
                .orEmpty()
                .filterNot { it in retainedPaths },
        )
        return response.copy(after = response.after?.let { signReviews(listOf(it)).single() })
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

    private suspend fun signReviews(reviews: List<ReviewResponse>): List<ReviewResponse> {
        val images =
            buildList {
                for (review in reviews) {
                    addAll(review.imagePaths.map { SignedImageRequest(bucket = BUCKET, path = it) })
                    review.avatarPath?.let { add(SignedImageRequest(bucket = AVATAR_BUCKET, path = it)) }
                }
            }
        val urls = fetchImageUrls(images)
        return reviews.map { review ->
            review.copy(
                imageReferences = review.imagePaths.map { MyReviewPhoto(urls[SignedImageRequest(BUCKET, it)]?.url, it, urls[SignedImageRequest(BUCKET, it)]?.expiresAt) },
                imageUrls = review.imagePaths.mapNotNull { urls[SignedImageRequest(BUCKET, it)]?.url },
                avatarUrl = avatarUrl(review.avatarPath, urls),
                avatarExpiresAt = review.avatarPath?.let { urls[SignedImageRequest(AVATAR_BUCKET, it)]?.expiresAt },
            )
        }
    }

    private suspend fun signMyReviews(reviews: List<MyReviewResponse>): List<MyReviewResponse> {
        val images =
            buildList {
                for (review in reviews) {
                    addAll(review.imagePaths.map { SignedImageRequest(bucket = BUCKET, path = it) })
                    review.authorAvatarPath?.let { add(SignedImageRequest(bucket = AVATAR_BUCKET, path = it)) }
                }
            }
        val urls = fetchImageUrls(images)
        return reviews.map { review ->
            review.copy(
                imageReferences = review.imagePaths.map { MyReviewPhoto(urls[SignedImageRequest(BUCKET, it)]?.url, it, urls[SignedImageRequest(BUCKET, it)]?.expiresAt) },
                imageUrls = review.imagePaths.map { urls[SignedImageRequest(BUCKET, it)]?.url },
                authorAvatarUrl = avatarUrl(review.authorAvatarPath, urls),
                authorAvatarExpiresAt = review.authorAvatarPath?.let { urls[SignedImageRequest(AVATAR_BUCKET, it)]?.expiresAt },
            )
        }
    }

    private suspend fun fetchImageUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> =
        signing.withLock {
            val userId = client.auth.currentUserOrNull()?.id
            val journal = changes.journal.value
            if (signedUserId != userId || signedEpoch != journal.epoch || journal.hasGapAfter(signedRevision)) {
                signedUrls.clear()
                signedUserId = userId
                signedEpoch = journal.epoch
            }
            for (record in journal.changesAfter(signedRevision)) {
                val access = record.change as? ReviewChange.AccessChanged ?: continue
                signedUrls.keys.filter { it.path.startsWith("${access.userId}/") }.forEach(signedUrls::remove)
            }
            signedRevision = journal.revision
            val unique = images.distinct()
            val expiresAfter = Clock.System.now() + SIGN_REFRESH_MARGIN
            val missing = unique.filter { signedUrls[it]?.expiresAt?.let { expiry -> expiry > expiresAfter } != true }
            if (missing.isNotEmpty()) {
                val fetched = imageUrlDataSource.fetchUrls(missing)
                check(client.auth.currentUserOrNull()?.id == userId && changes.journal.value.epoch == journal.epoch) { "Review image session changed" }
                val latest = changes.journal.value
                val accessChanged =
                    latest.hasGapAfter(journal.revision) ||
                        latest.changesAfter(journal.revision).any { record ->
                            val access = record.change as? ReviewChange.AccessChanged
                            access != null && unique.any { it.path.startsWith("${access.userId}/") }
                        }
                check(!accessChanged) { "Review image access changed" }
                for ((key, value) in fetched) {
                    if (value != null) signedUrls[key] = value else signedUrls.remove(key)
                }
                while (signedUrls.size > SIGN_CACHE_LIMIT) signedUrls.remove(signedUrls.keys.first())
            }
            unique.associateWith { signedUrls[it] }
        }

    private fun avatarUrl(
        path: String?,
        urls: Map<SignedImageRequest, SignedImageUrl?>,
    ): String? = path?.let { urls[SignedImageRequest(bucket = AVATAR_BUCKET, path = it)]?.url }

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

    private suspend fun cleanupIfRejected(
        paths: List<String>,
        error: Throwable,
    ) {
        val status = (error as? RestException)?.statusCode
        if (status in CONFIRMED_REJECTION_STATUSES) cleanup(paths)
    }

    private companion object {
        const val SIGN_CACHE_LIMIT = 512
        val SIGN_REFRESH_MARGIN = 1.minutes
        val CONFIRMED_REJECTION_STATUSES = setOf(400, 401, 403, 404, 409, 422, 429)
        const val RPC_FETCH_MY_REVIEWS = "fetch_my_reviews"
        const val RPC_FETCH_MY_REVIEW = "fetch_my_review"
        const val RPC_FETCH_SHOP_REVIEWS = "fetch_shop_reviews"
        const val RPC_FETCH_SHOP_REVIEWS_PAGE = "fetch_shop_reviews_page"
        const val RPC_FETCH_BLOCKED_SHOP_REVIEW_ONCE = "fetch_blocked_shop_review_once"
        const val RPC_FETCH_PROFILE_REVIEWS = "fetch_profile_reviews"
        const val RPC_SUBMIT_SHOP_REVIEW = "submit_shop_review_v2"
        const val RPC_FETCH_EDITABLE_REVIEW = "fetch_editable_shop_review"
        const val RPC_UPDATE_REVIEW = "update_shop_review_v2"
        const val RPC_DELETE_REVIEW = "delete_shop_review_v2"
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
    }
}
