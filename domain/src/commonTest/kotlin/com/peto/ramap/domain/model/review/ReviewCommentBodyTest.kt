package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.review.comment.ReviewCommentBody
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewCommentBodyTest {
    @Test
    fun `댓글은 앞뒤 공백을 제외하고 한 글자부터 500 코드 포인트까지 작성한다`() {
        assertFalse(ReviewCommentBody.isValid("　\t"))
        assertTrue(ReviewCommentBody.isValid(" 맛 "))
        assertTrue(ReviewCommentBody.isValid("　" + "🍜".repeat(500) + "\n"))
        assertFalse(ReviewCommentBody.isValid("🍜".repeat(501)))
        assertFalse(ReviewCommentBody.isValid("씨.발"))
    }
}
