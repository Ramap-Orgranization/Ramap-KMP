package com.peto.ramap.domain.model.moderation

internal object KoreanProfanityNormalizer {
    fun normalize(text: String): String {
        val compact =
            buildString {
                for (character in text) {
                    val normalized = normalizeCharacter(character)
                    if (normalized.isLetterOrDigit() || normalized in COMPATIBILITY_JAMO || normalized.isSurrogate()) {
                        append(normalized)
                    }
                }
            }
        return composeHangul(compact)
    }

    private fun normalizeCharacter(character: Char): Char =
        when (character) {
            in FULL_WIDTH_ASCII -> (character.code - FULL_WIDTH_OFFSET).toChar().lowercaseChar()
            in LEADING_JAMO -> INITIALS[character.code - LEADING_JAMO.first.code]
            in VOWEL_JAMO -> VOWELS[character.code - VOWEL_JAMO.first.code]
            in TRAILING_JAMO -> FINALS[character.code - TRAILING_JAMO.first.code + 1]
            else -> character.lowercaseChar()
        }

    private fun composeHangul(text: String): String =
        buildString {
            var index = 0
            while (index < text.length) {
                val initial = INITIALS.indexOf(text[index])
                val vowel = VOWELS.indexOf(text.getOrNull(index + 1) ?: NO_FINAL)
                if (initial < 0 || vowel < 0) {
                    append(text[index++])
                    continue
                }
                val final = finalIndex(text, index + 2)
                append((HANGUL_BASE + (initial * VOWELS.length + vowel) * FINALS.length + final).toChar())
                index += if (final == 0) 2 else 3
            }
        }

    private fun finalIndex(
        text: String,
        index: Int,
    ): Int {
        val candidate = text.getOrNull(index) ?: return 0
        val next = text.getOrNull(index + 1)
        if (candidate in INITIALS && next != null && next in VOWELS) return 0
        return FINALS.indexOf(candidate).coerceAtLeast(0)
    }

    private const val INITIALS = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    private const val VOWELS = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    private const val FINALS = "\u0000ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"
    private const val NO_FINAL = '\u0000'
    private const val HANGUL_BASE = 0xAC00
    private const val FULL_WIDTH_OFFSET = 0xFEE0
    private val FULL_WIDTH_ASCII = '\uFF01'..'\uFF5E'
    private val COMPATIBILITY_JAMO = '\u3131'..'\u318E'
    private val LEADING_JAMO = '\u1100'..'\u1112'
    private val VOWEL_JAMO = '\u1161'..'\u1175'
    private val TRAILING_JAMO = '\u11A8'..'\u11C2'
}
