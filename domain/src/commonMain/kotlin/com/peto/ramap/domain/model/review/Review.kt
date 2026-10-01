package com.peto.ramap.domain.model.review

import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.domain.model.moderation.KoreanProfanityFilter

data class Review(
    val id: String,
    val shopId: String,
    val body: String,
    val createdAt: String,
    val imageUrls: List<String> = emptyList(),
    val author: ReviewAuthor = ReviewAuthor("", ""),
    val moderationStatus: ReviewModerationStatus = ReviewModerationStatus.PUBLISHED,
    val isPublic: Boolean = true,
    val visitNumber: Int = 1,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isBlocked: Boolean = false,
) {
    companion object {
        private val profanityFilter = KoreanProfanityFilter()

        fun isValidBody(body: String): Boolean = codePointCount(body.trim()) in BODY_LENGTH && !containsProfanity(body)

        fun containsProfanity(body: String): Boolean = profanityFilter.containsProfanity(body)

        fun bodyCharacterCount(body: String): Int = codePointCount(body)

        val BODY_LENGTH = 5..30

        private fun codePointCount(value: String): Int {
            var count = 0
            var index = 0
            while (index < value.length) {
                index += if (value[index].isHighSurrogate() && value.getOrNull(index + 1)?.isLowSurrogate() == true) 2 else 1
                count++
            }
            return count
        }
    }
}
