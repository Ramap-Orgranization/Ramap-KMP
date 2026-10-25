package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.ProfileReviewResponse
import com.peto.ramap.data.model.RamenShopResponse
import com.peto.ramap.data.model.ReviewResponse
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.fake.FakeCommunityDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultUserProfileRepositoryTest {
    @Test
    fun `응답의 리뷰와 매장 정보를 순서대로 변환한다`() =
        runTest {
            val reviews = listOf(review("first", "shop-a"), review("second", "shop-b"), review("third", "shop-a"))
            val community = FakeCommunityDataSource(reviews)
            val repository = DefaultUserProfileRepository(community)

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 20L)).data

            assertEquals(reviews.map { it.review.toDomain() }, page.map { it.review })
            assertEquals(listOf("shop-a", "shop-b", "shop-a"), page.map { it.shop.id })
            assertEquals(listOf("author" to 20L), community.requestedReviewPages)
        }

    @Test
    fun `빈 리뷰 페이지를 반환한다`() =
        runTest {
            val repository = DefaultUserProfileRepository(FakeCommunityDataSource())

            val page = assertIs<RamapResult.Success<List<ProfileReview>>>(repository.fetchUserReviews("author", 0L)).data

            assertTrue(page.isEmpty())
        }

    @Test
    fun `리뷰 조회가 실패하면 오류를 반환한다`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = IllegalStateException("reviews unavailable"))
            val repository = DefaultUserProfileRepository(community)

            assertIs<RamapResult.Error>(repository.fetchUserReviews("author", 0L))
        }

    @Test
    fun `리뷰 조회 취소를 전파한다`() =
        runTest {
            val community = FakeCommunityDataSource(reviewError = CancellationException("cancelled"))
            val repository = DefaultUserProfileRepository(community)

            assertFailsWith<CancellationException> { repository.fetchUserReviews("author", 0L) }
        }

    private fun review(
        id: String,
        shopId: String,
    ) = ProfileReviewResponse(
        shop = shop(shopId),
        review =
            ReviewResponse(
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
            ),
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
