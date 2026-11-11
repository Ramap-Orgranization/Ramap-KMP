package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewModerationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewTest {
    @Test
    fun `본인 공개 리뷰에는 프로필도 공개일 때만 좋아요를 허용한다`() {
        val review = Review("review", "shop", "맛있는 라멘", "2026-11-11", author = ReviewAuthor("author", "작성자"))

        assertTrue(review.allowsLikeFrom("author", viewerProfileIsPublic = true))
        assertFalse(review.allowsLikeFrom("author", viewerProfileIsPublic = false))
        assertFalse(review.allowsLikeFrom("author", viewerProfileIsPublic = null))
        assertTrue(review.allowsLikeFrom("reader", viewerProfileIsPublic = false))
        assertTrue(review.allowsLikeFrom(null, viewerProfileIsPublic = null))
        assertFalse(review.copy(isPublic = false).allowsLikeFrom("author", viewerProfileIsPublic = true))
        assertFalse(review.copy(moderationStatus = ReviewModerationStatus.PENDING).allowsLikeFrom("author", viewerProfileIsPublic = true))
        assertFalse(review.copy(isBlocked = true).allowsLikeFrom("reader", viewerProfileIsPublic = true))
    }

    @Test
    fun `사진이 있어도 입력한 본문의 길이와 비속어 조건을 지킨다`() {
        for (imageCount in 0..5) {
            assertFalse(Review.isValidContent("", imageCount))
            assertFalse(Review.isValidContent("  \t\n\u00a0\u2003", imageCount))
            assertFalse(Review.isValidContent("1234", imageCount))
            assertTrue(Review.isValidContent("  12345  ", imageCount))
            assertTrue(Review.isValidContent("🍜".repeat(500), imageCount))
            assertFalse(Review.isValidContent("🍜".repeat(501), imageCount))
            assertFalse(Review.isValidContent("씨발 맛없어요", imageCount))
        }
        assertFalse(Review.isValidContent("12345", -1))
        assertFalse(Review.isValidContent("12345", 6))
    }

    @Test
    fun isValidBodyRejectsProfanityWithinTheAllowedLength() {
        for (body in listOf("씨발 맛없어요", "씨.발 맛없어요", "ㅅㅂ 별로야", "씨발 맛없어요")) {
            assertFalse(Review.isValidBody(body), body)
            assertTrue(Review.containsProfanity(body), body)
        }
        assertTrue(Review.isValidBody("국물이 미친 듯이 맛있어요"))
        assertTrue(Review.isValidBody("여행의 시발점이 된 라멘집"))
    }

    @Test
    fun `리뷰 본문은 앞뒤 공백을 제외한 Unicode 문자 5자부터 500자까지 허용한다`() {
        assertFalse(Review.isValidBody("1234"))
        assertTrue(Review.isValidBody("  12345  "))
        for (body in listOf("가".repeat(31), "가".repeat(500), "😀".repeat(500), "가😀".repeat(250))) {
            assertTrue(Review.isValidBody(body), body)
            assertTrue(Review.isValidBody("  $body  "), body)
        }
        assertFalse(Review.isValidBody("가".repeat(501)))
        assertFalse(Review.isValidBody("😀".repeat(501)))
    }

    @Test
    fun `500자 리뷰에서도 비속어를 거부한다`() {
        assertFalse(Review.isValidBody("가".repeat(498) + "씨발"))
    }

    @Test
    fun bodyCharacterCountCountsUnicodeCodePoints() {
        assertEquals(5, Review.bodyCharacterCount("😀😀abc"))
    }

    @Test
    fun defaultsToPublic() {
        assertTrue(Review("review", "shop", "body", "today").isPublic)
    }
}
