package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.EditableReviewResponse
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.ReviewLikeResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.domain.model.review.ReviewImage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultReviewRepositoryTest {
    @Test
    fun `비속어 리뷰는 데이터 소스 호출 전에 등록과 수정을 거부한다`() =
        runTest {
            val dataSource = FakeReviewDataSource()
            val repository = DefaultReviewRepository(dataSource, ReviewChangeNotifier())
            val submitted = assertIs<RamapResult.Error>(repository.submitReview("shop", "씨발 맛없어요", emptyList(), true))
            val updated = assertIs<RamapResult.Error>(repository.updateReview("review", "씨.발 맛없어요", emptyList(), emptyList(), false))
            assertIs<IllegalArgumentException>(assertIs<RamapError.Unknown>(submitted.error).cause)
            assertIs<IllegalArgumentException>(assertIs<RamapError.Unknown>(updated.error).cause)
            assertNull(dataSource.submittedReview)
        }

    @Test
    fun `비공개 리뷰 제출 시 공개 설정을 요청에 전달한다`() =
        runTest {
            val dataSource = FakeReviewDataSource()
            val repository = DefaultReviewRepository(dataSource, ReviewChangeNotifier())

            repository.submitReview("shop", "  review  ", emptyList(), isPublic = false).getOrThrow()

            assertEquals(
                ShopReviewRequest(
                    shopId = "shop",
                    body = "review",
                    imagePaths = emptyList(),
                    isPublic = false,
                ),
                dataSource.submittedReview,
            )
        }

    private class FakeReviewDataSource : ReviewDataSource {
        var submittedReview: ShopReviewRequest? = null

        override suspend fun fetchMyReviews(
            offset: Long,
            visibility: String,
        ): MyReviewsPageResponse = error("Unexpected my reviews fetch")

        override suspend fun fetchMyReview(reviewId: String): MyReviewResponse? = error("Unexpected my review fetch")

        override suspend fun fetchShopReviews(
            shopId: String,
            offset: Long,
        ): List<ReviewResponse> = emptyList()

        override suspend fun fetchBlockedShopReviewOnce(reviewId: String): ReviewResponse? = error("Unexpected blocked review fetch")

        override suspend fun fetchProfileReviews(
            userId: String?,
            offset: Long,
        ): List<ReviewResponse> = emptyList()

        override suspend fun submitReview(
            review: ShopReviewRequest,
            images: List<ReviewImage>,
        ) {
            submittedReview = review
        }

        override suspend fun fetchEditableReview(reviewId: String): Pair<EditableReviewResponse, List<String?>>? = error("Unexpected editable review fetch")

        override suspend fun updateReview(
            reviewId: String,
            body: String,
            retainedImagePaths: List<String>,
            newImages: List<ReviewImage>,
            isPublic: Boolean,
        ) {
            error("Unexpected review update")
        }

        override suspend fun deleteReview(reviewId: String) {
            error("Unexpected review deletion")
        }

        override suspend fun setReviewLike(
            reviewId: String,
            liked: Boolean,
        ): ReviewLikeResponse = error("Unexpected review like")
    }
}
