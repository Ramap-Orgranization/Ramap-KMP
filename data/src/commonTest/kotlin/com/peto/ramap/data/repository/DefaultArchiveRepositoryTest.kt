package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.fake.FakeMyReviewsDataSource
import com.peto.ramap.fake.FakeRamenShopDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DefaultArchiveRepositoryTest {
    @Test
    fun `모든 페이지를 서버 행수로 넘기며 중복 리뷰를 제거하고 사진 서명을 미룬다`() =
        runTest {
            val offsets = mutableListOf<Long>()
            val first = (1..30).map { review("$it", "shop") }
            val source =
                object : ReviewDataSource by FakeMyReviewsDataSource() {
                    override suspend fun fetchMyReviewsMetadata(offset: Long): MyReviewsPageResponse {
                        offsets += offset
                        return page(if (offset == 0L) first else listOf(first.last(), review("31", "shop")), 32)
                    }

                    override suspend fun fetchMyReviews(
                        offset: Long,
                        visibility: String,
                    ): MyReviewsPageResponse = error("must not resolve every review image")
                }
            val images = RecordingVisitedImages()
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop")))
            val result = DefaultArchiveRepository(source, shops, images).fetchArchiveShops().getOrThrow()

            assertEquals(listOf(0L, 30L), offsets)
            assertEquals(31, result.reviews.size)
            assertEquals(setOf("shop"), result.shops.keys)
            assertEquals(listOf(setOf("shop")), shops.requestedShopIdsHistory)
            assertTrue(images.requests.isEmpty())
        }

    @Test
    fun `매장 조회 실패는 빈 방문 기록 성공으로 바꾸지 않는다`() =
        runTest {
            val source =
                object : ReviewDataSource by FakeMyReviewsDataSource() {
                    override suspend fun fetchMyReviewsMetadata(offset: Long) = page(listOf(review("review", "shop")), 1)
                }
            val repository = DefaultArchiveRepository(source, FakeRamenShopDataSource(error = IllegalStateException("lookup failed")), RecordingVisitedImages())
            assertIs<RamapResult.Error>(repository.fetchArchiveShops())
        }

    @Test
    fun `여러 매장의 대표 사진 reference만 한 번에 서명한다`() =
        runTest {
            val images = RecordingVisitedImages()
            val repository = DefaultArchiveRepository(FakeMyReviewsDataSource(), FakeRamenShopDataSource(), images)
            val result = repository.resolvePhotos(setOf("owner/first.jpg", "owner/other-first.jpg")).getOrThrow()
            assertEquals(setOf("owner/first.jpg", "owner/other-first.jpg"), result.keys)
            assertEquals(1, images.batches)
            assertEquals(listOf(SignedImageRequest("shop-review-images", "owner/first.jpg"), SignedImageRequest("shop-review-images", "owner/other-first.jpg")), images.requests)
        }

    private fun review(
        id: String,
        shopId: String,
    ) = MyReviewResponse(
        id = id,
        shopId = shopId,
        shopName = shopId,
        body = "다녀온 매장 후기",
        createdAt = "2026-10-26T12:00:00Z",
        imagePaths = listOf("owner/$id.jpg"),
        moderationStatus = "published",
        isPublic = false,
    )

    private fun page(
        reviews: List<MyReviewResponse>,
        total: Int,
    ) = MyReviewsPageResponse(reviews, total, 0, total, false)

    private fun shop(id: String) =
        RamenShopResponse(
            id = id,
            name = id,
            address = "서울",
            lat = 37.0,
            lng = 127.0,
            createdAt = "2026-10-26T12:00:00Z",
            updatedAt = "2026-10-26T12:00:00Z",
        )
}

internal class RecordingVisitedImages : ImageUrlDataSource {
    val requests = mutableListOf<SignedImageRequest>()
    var batches = 0

    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
        batches += 1
        requests += images
        return images.associateWith { SignedImageUrl("https://example.com/${it.path}", Clock.System.now() + 5.minutes) }
    }
}
