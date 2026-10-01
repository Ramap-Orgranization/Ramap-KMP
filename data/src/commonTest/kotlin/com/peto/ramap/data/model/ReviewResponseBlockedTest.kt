package com.peto.ramap.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewResponseBlockedTest {
    @Test
    fun `blocked shop review response keeps only a placeholder flag`() {
        val response =
            Json.decodeFromString<ReviewResponse>(
                """{"id":"review","shop_id":"shop","body":"","created_at":"2026-09-30T00:00:00Z","image_paths":[],"user_id":"author","nickname":"","avatar_path":null,"is_blocked":true}""",
            )

        val review = response.toDomain()
        assertTrue(review.isBlocked)
        assertEquals("", review.body)
        assertEquals(emptyList(), review.imageUrls)
        assertEquals("", review.author.nickname)
        assertEquals(null, review.author.avatarUrl)
    }
}
