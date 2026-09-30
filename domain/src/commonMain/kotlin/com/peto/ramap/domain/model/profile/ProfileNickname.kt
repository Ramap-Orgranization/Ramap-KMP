package com.peto.ramap.domain.model.profile

import com.peto.ramap.domain.model.moderation.KoreanProfanityFilter
import kotlin.jvm.JvmInline

@JvmInline
value class ProfileNickname(
    val value: String,
) {
    val isValid: Boolean
        get() = isValid(value)

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 10

        private val allowed = Regex("^[A-Za-z0-9가-힣_]{$MIN_LENGTH,$MAX_LENGTH}$")
        private val profanityFilter = KoreanProfanityFilter()

        fun isValid(value: String): Boolean = allowed.matches(value) && !containsProfanity(value)

        fun containsProfanity(value: String): Boolean = profanityFilter.containsProfanity(value)
    }
}
