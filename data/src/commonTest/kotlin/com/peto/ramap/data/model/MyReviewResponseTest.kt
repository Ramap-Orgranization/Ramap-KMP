package com.peto.ramap.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class MyReviewResponseTest {
    @Test
    fun `내 리뷰 응답의 실제 좋아요 수와 저장된 방문 차수를 그대로 표시한다`() {
        val json = """{"id":"review","shop_id":"shop","shop_name":"매장","body":"방문 후기","created_at":"2026-10-27T00:00:00Z","image_paths":[],"moderation_status":"published","is_public":true,"visit_number":5,"like_count":8}"""
        val row = Json.decodeFromString<MyReviewResponse>(json).toDomain()

        assertEquals(5, row.review.visitNumber)
        assertEquals(8, row.review.likeCount)
    }

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

        assertEquals("https://example.com/avatar.jpg", review.review.author.avatarUrl)
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

        assertEquals(3, review.review.visitNumber)
        assertEquals(12, review.review.author.reviewCount)

        val uiReview = review.review
        assertEquals(3, uiReview.visitNumber)
        assertEquals(12, uiReview.author.reviewCount)
    }
}
