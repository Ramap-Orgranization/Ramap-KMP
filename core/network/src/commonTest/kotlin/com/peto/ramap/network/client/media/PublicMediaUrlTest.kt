package com.peto.ramap.network.client.media

import com.peto.ramap.network.config.RamapSecrets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PublicMediaUrlTest {
    private val configuredImageUrl =
        RamapSecrets.r2PublicBaseUrl.trimEnd('/').takeIf(String::isNotBlank)?.let {
            "$it/${MediaBucket.MENU_IMAGES_NAME}/$TEST_IMAGE_PATH"
        }

    @Test
    fun `공개 사진만 설정된 저장소의 논리 경로로 변환한다`() {
        assertEquals(
            configuredImageUrl,
            PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(TEST_IMAGE_PATH)).resolve(),
        )
        assertNull(PublicMediaUrl(MediaBucket(UNSUPPORTED_PRIVATE_BUCKET), MediaPath(PRIVATE_IMAGE_PATH)).resolve())
    }

    @Test
    fun `외부 사진 주소를 유지하고 자체 저장소 주소만 전환한다`() {
        assertEquals(EXTERNAL_IMAGE_URL, PublicMediaUrl.resolveOwnedUrl(EXTERNAL_IMAGE_URL))
        assertEquals(
            configuredImageUrl,
            PublicMediaUrl.resolveOwnedUrl(
                "${RamapSecrets.supabaseUrl.trimEnd('/')}${PublicMediaUrl.SUPABASE_PUBLIC_STORAGE_PATH}/${MediaBucket.MENU_IMAGES_NAME}/$TEST_IMAGE_PATH",
            ),
        )
    }

    @Test
    fun `공개 사진 설정이 없으면 이전 저장소 주소를 만들지 않는다`() {
        assertNull(PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(TEST_IMAGE_PATH), publicBaseUrl = "").resolve())
    }

    @Test
    fun `공개 사진은 설정된 CDN 도메인을 사용하고 개발용 R2 주소는 거절한다`() {
        assertEquals(
            "$CUSTOM_CDN_BASE/${MediaBucket.MENU_IMAGES_NAME}/$TEST_IMAGE_PATH",
            PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(TEST_IMAGE_PATH), CUSTOM_CDN_BASE).resolve(),
        )
        REJECTED_BASE_URLS.forEach { base ->
            assertNull(PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(TEST_IMAGE_PATH), base).resolve(), base)
        }
    }

    @Test
    fun `도메인 구매 없이 Worker 기본 주소로 공개 사진을 불러온다`() {
        assertEquals(
            "$WORKER_BASE_URL/${MediaBucket.MENU_IMAGES_NAME}/$TEST_IMAGE_PATH",
            PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(TEST_IMAGE_PATH), WORKER_BASE_URL).resolve(),
        )
        assertNull(PublicMediaUrl(MediaBucket(UNSUPPORTED_PROFILE_AVATARS_BUCKET), MediaPath(PRIVATE_IMAGE_PATH), WORKER_BASE_URL).resolve())
    }

    @Test
    fun `경로 탈출과 URL 주입을 공개 사진으로 변환하지 않는다`() {
        INVALID_PATHS.forEach { path ->
            assertNull(PublicMediaUrl(MediaBucket.MENU_IMAGES, MediaPath(path)).resolve(), path)
        }
    }

    companion object {
        private const val TEST_IMAGE_PATH = "shops/example.jpg"
        private const val PRIVATE_IMAGE_PATH = "owner/private.jpg"
        private const val UNSUPPORTED_PRIVATE_BUCKET = "shop-review-images"
        private const val UNSUPPORTED_PROFILE_AVATARS_BUCKET = "profile-avatars"
        private const val EXTERNAL_IMAGE_URL = "https://example.com/menu.jpg"
        private const val CUSTOM_CDN_BASE = "https://images.example.com"
        private const val WORKER_BASE_URL = "https://ramap-images.example-account.workers.dev"

        private val REJECTED_BASE_URLS =
            listOf(
                "http://images.example.com",
                "https://public.r2.dev",
                "https://user:secret@images.example.com",
                "https://images.example.com/other",
            )

        private val INVALID_PATHS =
            listOf(
                "../private.jpg",
                "/private.jpg",
                "a%2Fb.jpg",
                "a.jpg?token=secret",
                "https://example.com/a.jpg",
            )
    }
}
