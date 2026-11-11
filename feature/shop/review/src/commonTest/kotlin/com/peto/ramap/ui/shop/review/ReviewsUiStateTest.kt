package com.peto.ramap.ui.shop.review

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewModerationStatus
import com.peto.ramap.domain.model.review.ShopReviewsPage
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewsUiStateTest {
    @Test
    fun `사진을 리뷰 순서와 첨부 순서대로 모으고 빈 주소는 제외한다`() {
        val first = "https://example.com/first.jpg"
        val second = "https://example.com/second.jpg"
        val third = "https://example.com/third.jpg"
        val reviews =
            listOf(
                review("newer", listOf(first, "", second)),
                review("text-only", emptyList()),
                review("older", listOf(third)),
            )
        val state = ReviewsUiState(page = ShopReviewsPage(reviews, totalCount = 3))

        assertEquals(listOf(first, second, third), state.photoUrls)
    }

    @Test
    fun `차단한 리뷰의 사진은 리뷰를 일시 열람해도 모아보기에 포함하지 않는다`() {
        val blocked = review("blocked", listOf("https://example.com/blocked.jpg")).copy(isBlocked = true)
        val revealed = blocked.copy(imageUrls = listOf("https://example.com/revealed.jpg"))
        val state =
            ReviewsUiState(
                page = ShopReviewsPage(listOf(blocked), totalCount = 1),
                revealedBlockedReviews = mapOf(blocked.id to revealed),
            )

        assertTrue(state.photoUrls.isEmpty())
    }

    @Test
    fun `본인에게 조회된 비공개 및 검수 중 리뷰의 사진을 모아보기에 포함한다`() {
        val privatePhoto = "https://example.com/private.jpg"
        val pendingPhoto = "https://example.com/pending.jpg"
        val owner = ReviewAuthor(userId = "owner", nickname = "작성자")
        val privateReview = review("private", listOf(privatePhoto)).copy(author = owner, isPublic = false)
        val pendingReview = review("pending", listOf(pendingPhoto)).copy(author = owner, moderationStatus = ReviewModerationStatus.PENDING)
        val state =
            ReviewsUiState(
                page = ShopReviewsPage(listOf(privateReview, pendingReview), totalCount = 2),
                currentUserId = owner.userId,
            )

        assertEquals(listOf(privatePhoto, pendingPhoto), state.photoUrls)
    }

    private fun review(
        id: String,
        imageUrls: List<String>,
    ): Review =
        Review(
            id = id,
            shopId = "shop",
            body = "맛있는 라멘",
            createdAt = "2026-10-14T12:00:00Z",
            imageUrls = imageUrls,
        )
}
