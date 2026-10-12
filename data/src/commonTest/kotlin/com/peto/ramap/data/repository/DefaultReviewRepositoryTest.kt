package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.model.MyReviewResponse
import com.peto.ramap.data.model.MyReviewsPageResponse
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ShopReviewRequest
import com.peto.ramap.data.testReviewChanges
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.fake.FakeMyReviewsDataSource
import com.peto.ramap.fake.FakeRamenShopDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultReviewRepositoryTest {
    @Test
    fun `committed review remains successful when post commit signing or cleanup fails`() =
        runTest {
            val source = FakeMyReviewsDataSource().apply { hydrationFailure = IllegalStateException("signing unavailable") }
            val changes = testReviewChanges()
            val shop = shop("shop")
            val repository = DefaultReviewRepository(source, FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop)), changes, backgroundScope)
            assertIs<RamapResult.Success<Unit>>(repository.submitReview("shop", "리뷰 본문입니다", emptyList(), true))
            val written =
                assertIs<ReviewChange.Written>(
                    changes.journal.value.changes
                        .single()
                        .change,
                )
            assertEquals(shop.toDomain(), written.after?.shop)
            assertEquals("리뷰 본문입니다", source.submittedReview?.body)
        }

    @Test
    fun `new review publishes complete shop in written and hydrated changes without fetching review lists`() =
        runTest {
            val response = shop("new-shop").copy(instagramProfileImagePath = "shop/avatar.jpg", menuCategoryIds = listOf("shoyu"))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(response))
            val changes = testReviewChanges()
            val source = FakeMyReviewsDataSource()
            val repository = DefaultReviewRepository(source, shops, changes, backgroundScope)

            repository.submitReview("new-shop", "리뷰 본문입니다", emptyList(), false).getOrThrow()

            val written =
                assertIs<ReviewChange.Written>(
                    changes.journal.value.changes[0]
                        .change,
                )
            val hydrated =
                assertIs<ReviewChange.Hydrated>(
                    changes.journal.value.changes[1]
                        .change,
                )
            assertEquals(response.toDomain(), written.after?.shop)
            assertEquals(written.after?.shop, hydrated.snapshot.shop)
            assertEquals(listOf(setOf("new-shop")), shops.requestedShopIdsHistory)
        }

    @Test
    fun `temporary shop lookup failure is recovered during hydration without retrying the write`() =
        runTest {
            val delegate = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("new-shop")))
            var attempts = 0
            val shops =
                object : RamenShopDataSource by delegate {
                    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse> {
                        attempts += 1
                        if (attempts == 1) error("temporary shop lookup failure")
                        return delegate.fetchRamenShopsByIds(shopIds)
                    }
                }
            val changes = testReviewChanges()
            val repository = DefaultReviewRepository(FakeMyReviewsDataSource(), shops, changes, backgroundScope)

            repository.submitReview("new-shop", "리뷰 본문입니다", emptyList(), false).getOrThrow()

            val written =
                assertIs<ReviewChange.Written>(
                    changes.journal.value.changes[0]
                        .change,
                )
            val hydrated =
                assertIs<ReviewChange.Hydrated>(
                    changes.journal.value.changes[1]
                        .change,
                )
            assertNull(written.after?.shop)
            assertEquals(shop("new-shop").toDomain(), hydrated.snapshot.shop)
            assertEquals(2, attempts)
            assertEquals(
                1,
                changes.journal.value.changes
                    .count { it.change is ReviewChange.Written },
            )
            runCurrent()
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(2, attempts)
        }

    @Test
    fun `missing shop is retried in background and retains committed review success`() =
        runTest {
            val delegate = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("new-shop")))
            var attempts = 0
            val shops =
                object : RamenShopDataSource by delegate {
                    override suspend fun fetchRamenShopsByIds(shopIds: Set<String>): List<RamenShopResponse> {
                        attempts += 1
                        return if (attempts <= 2) emptyList() else delegate.fetchRamenShopsByIds(shopIds)
                    }
                }
            val changes = testReviewChanges()
            val source = FakeMyReviewsDataSource()
            val repository = DefaultReviewRepository(source, shops, changes, backgroundScope)

            assertIs<RamapResult.Success<Unit>>(repository.submitReview("new-shop", "리뷰 본문입니다", emptyList(), false))
            assertNull(
                assertIs<ReviewChange.Hydrated>(
                    changes.journal.value.changes
                        .last()
                        .change,
                ).snapshot.shop,
            )
            runCurrent()
            advanceTimeBy(1000)
            runCurrent()

            assertEquals(
                shop("new-shop").toDomain(),
                assertIs<ReviewChange.Hydrated>(
                    changes.journal.value.changes
                        .last()
                        .change,
                ).snapshot.shop,
            )
            assertEquals(3, attempts)
            assertEquals(
                1,
                changes.journal.value.changes
                    .count { it.change is ReviewChange.Written },
            )
            advanceTimeBy(10_000)
            runCurrent()
            assertEquals(3, attempts)
        }

    @Test
    fun `shop lookup cancellation after commit keeps write successful and published`() =
        runTest {
            val shops = FakeRamenShopDataSource(error = CancellationException("shop lookup cancelled"))
            val changes = testReviewChanges()
            val repository = DefaultReviewRepository(FakeMyReviewsDataSource(), shops, changes, backgroundScope)

            assertIs<RamapResult.Success<Unit>>(repository.submitReview("new-shop", "리뷰 본문입니다", emptyList(), false))

            assertTrue(
                changes.journal.value.changes
                    .any { it.change is ReviewChange.Written },
            )
        }

    @Test
    fun `single owner projection includes shop and skips shop lookup for absent review`() =
        runTest {
            val source = FakeMyReviewsDataSource(myReview = review("new-review", "new-shop"))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("new-shop")))
            val repository = DefaultReviewRepository(source, shops, testReviewChanges(), backgroundScope)

            assertEquals(shop("new-shop").toDomain(), repository.fetchMyReview("new-review").getOrThrow()?.shop)
            assertNull(repository.fetchMyReview("missing-review").getOrThrow())
            assertEquals(listOf(setOf("new-shop")), shops.requestedShopIdsHistory)
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
