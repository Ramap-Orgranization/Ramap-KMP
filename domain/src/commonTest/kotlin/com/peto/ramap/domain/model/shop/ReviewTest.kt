package com.peto.ramap.domain.model.shop

import com.peto.ramap.domain.model.review.Review
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewTest {
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
