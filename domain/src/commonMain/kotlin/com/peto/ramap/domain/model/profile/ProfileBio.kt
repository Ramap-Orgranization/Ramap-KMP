package com.peto.ramap.domain.model.profile

import kotlin.jvm.JvmInline

@JvmInline
value class ProfileBio(
    val value: String,
) {
    val isValid: Boolean
        get() = isValid(value)

    val length: Int
        get() = length(value)

    companion object {
        const val MAX_LENGTH = 50

        private const val lineBreaks = "\n\r\u000B\u000C\u0085\u2028\u2029"

        fun isValid(value: String): Boolean = length(value) <= MAX_LENGTH && value.none { it in lineBreaks }

        fun length(value: String): Int {
            var length = 0
            var index = 0
            while (index < value.length) {
                val isPair = value[index].isHighSurrogate() && index + 1 < value.length && value[index + 1].isLowSurrogate()
                index += if (isPair) 2 else 1
                length++
            }
            return length
        }
    }
}
