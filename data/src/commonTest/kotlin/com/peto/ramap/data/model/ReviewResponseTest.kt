package com.peto.ramap.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewResponseTest {
    @Test
    fun `탈퇴한 작성자의 프로필 연결만 제거하고 리뷰 신고를 허용한다`() {
        val response =
            Json.decodeFromString<ReviewResponse>(
                """{"id":"review","shop_id":"shop","body":"리뷰 본문","created_at":"2026-11-06T00:00:00Z","user_id":"","nickname":"탈퇴한 사용자","is_withdrawn":true,"moderation_status":"published"}""",
            )

        val review = response.toDomain()

        assertEquals("리뷰 본문", review.body)
        assertEquals("", review.author.userId)
        assertTrue(review.author.isWithdrawn)
        assertTrue(review.allowsReportFrom("viewer"))
        assertFalse(review.allowsDeletionBy("viewer"))
    }
}
