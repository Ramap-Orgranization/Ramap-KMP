package com.peto.ramap.data.datasource.review

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.ProfileReviewResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.domain.model.review.MyReviewPhoto

internal class ReviewImageResolver(
    private val imageUrlDataSource: ImageUrlDataSource,
) {
    suspend fun resolveProfileReviews(rows: List<ProfileReviewResponse>): List<ProfileReviewResponse> = rows.zip(resolveReviews(rows.map { it.review })) { row, signed -> row.copy(review = signed) }

    suspend fun resolveReviews(reviews: List<ReviewResponse>): List<ReviewResponse> {
        val images =
            buildList {
                for (review in reviews) {
                    addAll(review.imagePaths.map { SignedImageRequest(bucket = BUCKET, path = it) })
                    review.avatarPath?.let { add(SignedImageRequest(bucket = AVATAR_BUCKET, path = it)) }
                }
            }
        val urls = imageUrlDataSource.fetchUrls(images)
        return reviews.map { review ->
            review.copy(
                imageReferences = review.imagePaths.map { MyReviewPhoto(urls[SignedImageRequest(BUCKET, it)]?.url, it, urls[SignedImageRequest(BUCKET, it)]?.expiresAt) },
                imageUrls = review.imagePaths.mapNotNull { urls[SignedImageRequest(BUCKET, it)]?.url },
                avatarUrl = avatarUrl(review.avatarPath, urls),
                avatarExpiresAt = review.avatarPath?.let { urls[SignedImageRequest(AVATAR_BUCKET, it)]?.expiresAt },
            )
        }
    }

    suspend fun resolveMyReviews(reviews: List<MyReviewResponse>): List<MyReviewResponse> {
        val images =
            buildList {
                for (review in reviews) {
                    addAll(review.imagePaths.map { SignedImageRequest(bucket = BUCKET, path = it) })
                    review.authorAvatarPath?.let { add(SignedImageRequest(bucket = AVATAR_BUCKET, path = it)) }
                }
            }
        val urls = imageUrlDataSource.fetchUrls(images)
        return reviews.map { review ->
            review.copy(
                imageReferences = review.imagePaths.map { MyReviewPhoto(urls[SignedImageRequest(BUCKET, it)]?.url, it, urls[SignedImageRequest(BUCKET, it)]?.expiresAt) },
                imageUrls = review.imagePaths.map { urls[SignedImageRequest(BUCKET, it)]?.url },
                authorAvatarUrl = avatarUrl(review.authorAvatarPath, urls),
                authorAvatarExpiresAt = review.authorAvatarPath?.let { urls[SignedImageRequest(AVATAR_BUCKET, it)]?.expiresAt },
            )
        }
    }

    private fun avatarUrl(
        path: String?,
        urls: Map<SignedImageRequest, SignedImageUrl?>,
    ): String? = path?.let { urls[SignedImageRequest(bucket = AVATAR_BUCKET, path = it)]?.url }

    suspend fun resolveImagePaths(paths: List<String>): List<String?> {
        val images = paths.map { SignedImageRequest(bucket = BUCKET, path = it) }
        val urls = imageUrlDataSource.fetchUrls(images)
        return images.map { urls[it]?.url }
    }

    private companion object {
        const val BUCKET = "shop-review-images"
        const val AVATAR_BUCKET = "profile-avatars"
    }
}
