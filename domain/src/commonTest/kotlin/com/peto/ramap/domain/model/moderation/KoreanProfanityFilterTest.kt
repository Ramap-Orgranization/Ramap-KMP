package com.peto.ramap.domain.model.moderation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KoreanProfanityFilterTest {
    private val filter = KoreanProfanityFilter()

    @Test
    fun detectsExplicitProfanityAndCommonVariants() {
        for (text in listOf("씨발", "병신이네", "개새끼", "ㅅㅂ", "ㅂㅅ", "ㅈㄹ", "씨1발", "SIBAL", "🖕🏽")) {
            assertTrue(filter.containsProfanity(text), text)
        }
    }

    @Test
    fun detectsWhitespacePunctuationAndInvisibleSeparators() {
        for (text in listOf("씨 발", "씨.발", "씨_발", "씨\n발", "씨\u200B발", "씨\u200D발", "ㅅ / ㅂ")) {
            assertTrue(filter.containsProfanity(text), text)
        }
    }

    @Test
    fun detectsFullWidthLatinAndDecomposedHangul() {
        for (text in listOf("ＳＩＢＡＬ", "ｓ１ｂａｌ", "씨발", "ㅆㅣㅂㅏㄹ", "병신", "ㅂㅕㅇㅅㅣㄴ")) {
            assertTrue(filter.containsProfanity(text), text)
        }
    }

    @Test
    fun preservesNormalFoodReviewsNumbersAndJamoBoundaries() {
        for (text in listOf("", "국물이 미친 듯이 맛있어요", "다시 방문하고 싶어요", "먹어보지 못했어요", "시바견", "새끼손가락", "밥상", "18시 방문", "가격 18000원", "저녁 세끼", "친절해요 ㅎㅎ")) {
            assertFalse(filter.containsProfanity(text), text)
        }
    }

    @Test
    fun allowsKnownExceptionsOnlyAtTheirOwnLocations() {
        assertFalse(filter.containsProfanity("여행의 시발점이 된 라멘집"))
        assertFalse(filter.containsProfanity("시 발 역"))
        assertTrue(filter.containsProfanity("시발점 씨발"))
        assertTrue(filter.containsProfanity("시발 시발점"))
    }

    @Test
    fun allowsCustomWordsAndExceptionsWithoutJoiningTextAcrossExceptions() {
        val custom =
            KoreanProfanityFilter(
                blockedWords = listOf("금지어", "", "   ", "금지어"),
                allowedExpressions = listOf("금지어설명", "허용"),
            )
        assertTrue(custom.containsProfanity("금.지.어"))
        assertFalse(custom.containsProfanity("금지어설명"))
        assertFalse(custom.containsProfanity("금허용지어"))
        assertTrue(custom.containsProfanity("금지어설명 금지어"))
        assertFalse(custom.containsProfanity("씨발"))
    }

    @Test
    fun ignoresEmptyExceptionsAndCopiesCallerCollections() {
        val words = mutableListOf("금지어")
        val exceptions = mutableListOf("")
        val custom = KoreanProfanityFilter(blockedWords = words, allowedExpressions = exceptions)
        words.clear()
        exceptions.add("금지어")
        assertTrue(custom.containsProfanity("금지어"))
        assertFalse(KoreanProfanityFilter(emptyList()).containsProfanity("씨발"))
    }
}
