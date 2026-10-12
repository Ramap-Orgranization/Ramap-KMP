package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.data.testReviewChanges
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
    fun `리뷰의 매장은 ID를 중복 제거해 한 번 조회하고 리뷰 순서를 유지한다`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"), review("third", "shop-a"))
            val community = FakeCommunityDataSource(reviews)
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b"), shop("shop-a")))
            val repository = DefaultCommunityRepository(community, shops, changes = testReviewChanges())

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 20L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertEquals(listOf("shop-a", "shop-b", "shop-a"), page.map { it.shop?.id })
            assertEquals(listOf(setOf("shop-a", "shop-b")), shops.requestedShopIdsHistory)
            assertEquals(listOf("author" to 20L), community.requestedReviewPages)
        }

    @Test
    fun `매장이 없어도 해당 리뷰는 목록에 유지한다`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"))
            val shops = FakeRamenShopDataSource(fetchByIdsResponses = listOf(shop("shop-b")))
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(reviews), shops, changes = testReviewChanges())

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertNull(page.first().shop)
            assertEquals("shop-b", page.last().shop?.id)
        }

    @Test
    fun `매장 조회가 실패해도 매장 정보를 제외한 모든 리뷰를 유지한다`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"))
            val shops = FakeRamenShopDataSource(error = IllegalStateException("shops unavailable"))
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(reviews), shops, changes = testReviewChanges())

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertEquals(reviews.map { it.toDomain() }, page.map { it.review })
            assertTrue(page.all { it.shop == null })
        }

    @Test
    fun `빈 리뷰 페이지는 매장을 조회하지 않는다`() =
        runTest {
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(FakeCommunityDataSource(), shops, changes = testReviewChanges())

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertTrue(page.isEmpty())
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    @Test
    fun `리뷰 조회가 실패하면 오류를 반환하고 매장은 조회하지 않는다`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = IllegalStateException("reviews unavailable"))
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(community, shops, changes = testReviewChanges())

            assertIs<RamapResult.Error>(repository.fetchUserReviews("author", 0L))
            assertTrue(shops.requestedShopIdsHistory.isEmpty())
        }

    @Test
    fun `매장 조회 취소는 그대로 전파한다`() =
        runTest {
            val community = FakeCommunityDataSource(listOf(review("first", "shop-a")))
            val shops = FakeRamenShopDataSource(error = CancellationException("cancelled"))
            val repository = DefaultCommunityRepository(community, shops, changes = testReviewChanges())

            assertFailsWith<CancellationException> { repository.fetchUserReviews("author", 0L) }
        }

    @Test
    fun `리뷰 조회 취소를 전파하고 매장을 조회하지 않는다`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = CancellationException("cancelled"))
            val shops = FakeRamenShopDataSource()
            val repository = DefaultCommunityRepository(community, shops, changes = testReviewChanges())

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
