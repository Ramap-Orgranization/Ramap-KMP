package com.peto.ramap.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MyReviewResponseTest {
    @Test
    fun `작성자 아바타 URL을 내 리뷰에 매핑한다`() {
        val review =
            MyReviewResponse(
                id = "review",
                shopId = "shop",
                shopName = "매장",
                body = "리뷰 본문",
                createdAt = "2026-09-30T00:00:00Z",
                imagePaths = emptyList(),
                moderationStatus = "published",
                isPublic = true,
                authorAvatarUrl = "https://example.com/avatar.jpg",
            ).toDomain()

        assertEquals("https://example.com/avatar.jpg", review.authorAvatarUrl)
    }

    @Test
    fun `방문 회차와 작성자 리뷰 개수를 내 리뷰에 매핑한다`() {
        val review =
            MyReviewResponse(
                id = "review",
                shopId = "shop",
                shopName = "매장",
                body = "리뷰 본문",
                createdAt = "2026-09-30T00:00:00Z",
                imagePaths = emptyList(),
                moderationStatus = "published",
                isPublic = true,
                visitNumber = 3,
                authorReviewCount = 12,
            ).toDomain()

        assertEquals(3, review.visitNumber)
        assertEquals(12, review.authorReviewCount)

        val uiReview = review.toReview()
        assertEquals(3, uiReview.visitNumber)
        assertEquals(12, uiReview.author.reviewCount)
    }
}
