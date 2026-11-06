package com.peto.ramap.network.client.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaBucketTest {
    @Test
    fun `공개 버킷인지 올바르게 판별한다`() {
        assertTrue(MediaBucket.SHOP_PROFILE_IMAGES.isPublic, "shop-profile-images는 공개 버킷이어야 한다.")
        assertTrue(MediaBucket.EVENT_IMAGES.isPublic, "event-images는 공개 버킷이어야 한다.")
        assertTrue(MediaBucket.MENU_IMAGES.isPublic, "menu-images는 공개 버킷이어야 한다.")
        assertFalse(MediaBucket("private-images").isPublic, "비공개 버킷은 isPublic이 false여야 한다.")
    }

    @Test
    fun `버킷 이름의 기본 형식 유효성을 올바르게 검사한다`() {
        assertTrue(MediaBucket("valid-bucket").isValid, "올바른 버킷 이름이어야 한다.")
        assertFalse(MediaBucket("").isValid, "빈 문자열은 유효하지 않아야 한다.")
        assertFalse(MediaBucket("   ").isValid, "공백 문자열은 유효하지 않아야 한다.")
        assertFalse(MediaBucket("invalid/bucket").isValid, "경로 구분자 /가 포함되면 유효하지 않아야 한다.")
        assertFalse(MediaBucket("invalid\\bucket").isValid, "경로 구분자 \\가 포함되면 유효하지 않아야 한다.")
        assertFalse(MediaBucket("..").isValid, "경로 탐색 문자열은 유효하지 않아야 한다.")
    }

    @Test
    fun `공개 버킷 이름으로부터 MediaBucket 객체를 생성한다`() {
        assertEquals(
            MediaBucket.SHOP_PROFILE_IMAGES,
            MediaBucket.fromPublic("shop-profile-images"),
            "shop-profile-images 이름으로 생성 성공해야 한다.",
        )
        assertNull(MediaBucket.fromPublic("private-bucket"), "비공개 버킷 이름은 null을 반환해야 한다.")
        assertNull(MediaBucket.fromPublic(""), "빈 문자열은 null을 반환해야 한다.")
        assertNull(MediaBucket.fromPublic(null), "null 입력은 null을 반환해야 한다.")
    }
}
