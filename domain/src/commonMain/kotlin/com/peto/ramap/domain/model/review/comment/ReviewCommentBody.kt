package com.peto.ramap.domain.model.review.comment

import com.peto.ramap.domain.model.review.Review

object ReviewCommentBody {
    const val MAX_LENGTH = 500

    fun isValid(body: String): Boolean = Review.bodyCharacterCount(body.trim()) in 1..MAX_LENGTH && !Review.containsProfanity(body)
}
