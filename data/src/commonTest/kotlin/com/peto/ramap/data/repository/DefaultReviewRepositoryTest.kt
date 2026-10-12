package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.testReviewChanges
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.fake.FakeMyReviewsDataSource
import com.peto.ramap.fake.FakeRamenShopDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultReviewRepositoryTest {
    @Test
    fun `committed review remains successful when post commit signing or cleanup fails`() =
        runTest {
            val source = FakeMyReviewsDataSource().apply { hydrationFailure = IllegalStateException("signing unavailable") }
            val changes = testReviewChanges()
            val repository = DefaultReviewRepository(source, FakeRamenShopDataSource(), changes)
            assertIs<RamapResult.Success<Unit>>(repository.submitReview("shop", "리뷰 본문입니다", emptyList(), true))
            assertTrue(
                changes.journal.value.changes
                    .any { it.change is com.peto.ramap.domain.model.review.change.ReviewChange.Written },
            )
            assertEquals("리뷰 본문입니다", source.submittedReview?.body)
        }

    @Test
    fun `비속어 리뷰는 데이터 소스 호출 전에 등록과 수정을 거부한다`() =
        runTest {
            val dataSource = FakeMyReviewsDataSource()
            val repository = DefaultReviewRepository(dataSource, FakeRamenShopDataSource(), changes = testReviewChanges())
            val submitted = assertIs<RamapResult.Error>(repository.submitReview("shop", "씨발 맛없어요", emptyList(), true))
            val updated = assertIs<RamapResult.Error>(repository.updateReview("review", "씨.발 맛없어요", emptyList(), emptyList(), false))
            assertIs<IllegalArgumentException>(assertIs<RamapError.Unknown>(submitted.error).cause)
            assertIs<IllegalArgumentException>(assertIs<RamapError.Unknown>(updated.error).cause)
            assertNull(dataSource.submittedReview)
        }

    @Test
    fun `비공개 리뷰 제출 시 공개 설정을 요청에 전달한다`() =
        runTest {
            val dataSource = FakeMyReviewsDataSource()
            val repository = DefaultReviewRepository(dataSource, FakeRamenShopDataSource(), changes = testReviewChanges())

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

    @Test
    fun `my reviews enrich shops in one deduplicated nonblank batch and preserve page metadata`() =
        runTest {
            val response = page(listOf(review("first", "shop-a"), review("second", "shop-b"), review("third", "shop-a"), review("blank", "")))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b"), shop("shop-a")))
            val repository = DefaultReviewRepository(FakeMyReviewsDataSource(response), shops, changes = testReviewChanges())

            val result = repository.fetchMyReviews(30L, MyReviewVisibility.ALL).getOrThrow()

            assertEquals(listOf("shop-a", "shop-b", "shop-a", null), result.reviews.map { it.shop?.id })
            assertEquals(response.toDomain(), result.copy(reviews = result.reviews.map { it.copy(shop = null) }))
            assertEquals(listOf(setOf("shop-a", "shop-b")), shops.requestedShopIdsHistory)
        }

    @Test
    fun `missing my review shops retain reviews and shop names`() =
        runTest {
            val response = page(listOf(review("first", "missing"), review("second", "shop-b")))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b")))
            val result =
                DefaultReviewRepository(FakeMyReviewsDataSource(response), shops, changes = testReviewChanges())
                    .fetchMyReviews(0L, MyReviewVisibility.ALL)
                    .getOrThrow()

            assertNull(result.reviews.first().shop)
            assertEquals("missing", result.reviews.first().shopName)
            val lastReview = result.reviews.last()
            assertEquals("shop-b", lastReview.shop?.id)
        }

    @Test
    fun `shop lookup failure preserves my reviews page`() =
        runTest {
            val response = page(listOf(review("first", "shop-a")))
            val shops = FakeRamenShopDataSource(error = IllegalStateException("shops unavailable"))
            val result =
                DefaultReviewRepository(FakeMyReviewsDataSource(response), shops, changes = testReviewChanges())
                    .fetchMyReviews(0L, MyReviewVisibility.ALL)
                    .getOrThrow()

            assertEquals(response.toDomain(), result)
        }

    @Test
    fun `empty and blank shop ids skip my review shop lookup`() =
        runTest {
            val shops = FakeRamenShopDataSource()
            for (reviews in listOf(emptyList(), listOf(review("blank", " ")))) {
                val response = page(reviews)
                val result =
                    DefaultReviewRepository(FakeMyReviewsDataSource(response), shops, changes = testReviewChanges())
                        .fetchMyReviews(0L, MyReviewVisibility.ALL)
                        .getOrThrow()
                assertEquals(response.toDomain(), result)
            }
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    @Test
    fun `shop lookup cancellation propagates from my reviews`() =
        runTest {
            val shops = FakeRamenShopDataSource(error = CancellationException("cancelled"))
            val repository = DefaultReviewRepository(FakeMyReviewsDataSource(page(listOf(review("first", "shop-a")))), shops, changes = testReviewChanges())

            assertFailsWith<CancellationException> { repository.fetchMyReviews(0L, MyReviewVisibility.ALL) }
        }

    private fun page(reviews: List<MyReviewResponse>) =
        MyReviewsPageResponse(
            reviews = reviews,
            totalCount = 80,
            publicCount = 50,
            privateCount = 30,
            profileIsPublic = false,
        )

    private fun review(
        id: String,
        shopId: String,
    ) = MyReviewResponse(
        id = id,
        shopId = shopId,
        shopName = shopId,
        body = "라멘 후기",
        createdAt = "2026-10-12T12:00:00Z",
        imagePaths = emptyList(),
        moderationStatus = "approved",
        isPublic = true,
    )

    private fun shop(id: String) =
        RamenShopResponse(
            id = id,
            name = id,
            address = "서울 마포구",
            lat = 37.0,
            lng = 127.0,
            createdAt = "2026-10-12T12:00:00Z",
            updatedAt = "2026-10-12T12:00:00Z",
        )
}
