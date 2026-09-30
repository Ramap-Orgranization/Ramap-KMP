package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.review.Review
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewTest {
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
    fun isValidBody_countsUnicodeCodePointsAfterTrimming() {
        assertFalse(Review.isValidBody("1234"))
        assertTrue(Review.isValidBody("  12345  "))
        assertTrue(Review.isValidBody("😀".repeat(30)))
        assertFalse(Review.isValidBody("😀".repeat(31)))
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
