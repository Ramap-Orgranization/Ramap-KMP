package com.peto.ramap.domain.model.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileValidationTest {
    @Test
    fun nicknameRequiresTwoToTenAllowedCharacters() {
        listOf("시오", "ramen_1234", "가".repeat(10)).forEach { assertTrue(ProfileNickname.isValid(it)) }
        listOf("", "가", "가".repeat(11), " 라멘", "라멘 ", "라 멘", "라멘🍜", "ㄱㄴ").forEach {
            assertFalse(ProfileNickname.isValid(it))
        }
    }

    @Test
    fun bioAllowsEmptyAndFiftyCodepointsOnOneLine() {
        for (bio in listOf("", "라멘을 좋아해요", "가".repeat(50), "🍜".repeat(50))) {
            assertTrue(ProfileBio.isValid(bio))
        }
        assertEquals(50, ProfileBio.length("🍜".repeat(50)))
        assertEquals(3, ProfileBio.length("가🍜a"))
        assertFalse(ProfileBio.isValid("🍜".repeat(51)))
        for (separator in "\n\r\u000B\u000C\u0085\u2028\u2029") {
            assertFalse(ProfileBio.isValid("한 줄${separator}두 줄"))
        }
    }

    @Test
    fun imageValidatesBytesMimeAndSize() {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        assertTrue(ProfileImage(jpeg, "image/jpeg").isValid())
        assertTrue(ProfileImage(png, "image/png").isValid())
        assertTrue(ProfileImage(jpeg.copyOf(ProfileImage.MAX_BYTES), "image/jpeg").isValid())
        assertFalse(ProfileImage(jpeg.copyOf(ProfileImage.MAX_BYTES + 1), "image/jpeg").isValid())
        assertFalse(ProfileImage(jpeg, "image/png").isValid())
        assertFalse(ProfileImage(png, "image/gif").isValid())
        assertFalse(ProfileImage(byteArrayOf(), "image/jpeg").isValid())
    }
}
