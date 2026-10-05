package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.fake.FakeCommunityDataSource
import com.peto.ramap.fake.FakeRamenShopDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultCommunityRepositoryTest {
    @Test
    fun `reviews match shops by id with a single deduplicated batch and retain order`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"), review("third", "shop-a"))
            val community = FakeCommunityDataSource(reviews)
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b"), shop("shop-a")))
            val repository = DefaultCommunityRepository(community, ReviewChangeNotifier(), shops)

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 20L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertEquals(listOf("shop-a", "shop-b", "shop-a"), page.map { it.shop?.id })
            assertEquals(listOf(setOf("shop-a", "shop-b")), shops.requestedShopIdsHistory)
            assertEquals(listOf("author" to 20L), community.requestedReviewPages)
        }

    @Test
    fun `missing shops leave their reviews in the page`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b")))
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(reviews), ReviewChangeNotifier(), shops)

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertNull(page.first().shop)
            assertEquals("shop-b", page.last().shop?.id)
        }

    @Test
    fun `shop request failure preserves every review without shop information`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"))
            val shops = FakeRamenShopDataSource(error = IllegalStateException("shops unavailable"))
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(reviews), ReviewChangeNotifier(), shops)

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertTrue(page.all { it.shop == null })
        }

    @Test
    fun `empty review page skips the shop request`() =
        runTest {
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(), ReviewChangeNotifier(), shops)

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertTrue(page.isEmpty())
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    @Test
    fun `review failure returns an error and skips shop lookup`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = IllegalStateException("reviews unavailable"))
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(community, ReviewChangeNotifier(), shops)

            assertIs<RamapResult.Error>(repository.fetchUserReviews("author", 0L))
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    @Test
    fun `shop cancellation propagates instead of returning a fallback`() =
        runTest {
            val community = FakeCommunityDataSource(listOf(review("first", "shop-a")))
            val shops = FakeRamenShopDataSource(error = CancellationException("cancelled"))
            val repository = DefaultCommunityRepository(community, ReviewChangeNotifier(), shops)

            assertFailsWith<CancellationException> { repository.fetchUserReviews("author", 0L) }
        }

    @Test
    fun `review cancellation propagates and skips shop lookup`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = CancellationException("cancelled"))
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(community, ReviewChangeNotifier(), shops)

            assertFailsWith<CancellationException> { repository.fetchUserReviews("author", 0L) }
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    private fun review(
        id: String,
        shopId: String,
    ) = ReviewResponse(
        id = id,
        shopId = shopId,
        body = "라멘 후기",
        createdAt = "2026-09-30T12:00:00Z",
        imageUrls = listOf("review-image"),
        userId = "author",
        nickname = "라멘팬",
        visitNumber = 2,
        likeCount = 3,
        isLiked = true,
    )

    private fun shop(id: String) =
        RamenShopResponse(
            id = id,
            name = id,
            address = "서울 마포구",
            lat = 37.0,
            lng = 127.0,
            createdAt = "2026-09-30T12:00:00Z",
            updatedAt = "2026-09-30T12:00:00Z",
        )
}
