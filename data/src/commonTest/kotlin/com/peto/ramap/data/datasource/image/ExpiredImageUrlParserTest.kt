package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ExpiredImageUrlParserTest {
    private val parser = ExpiredImageUrlParser(SUPABASE_BASE, ACCOUNT_ID, PRIVATE_BUCKET)
    private val now = Instant.parse("2026-11-03T12:05:00Z")

    @Test
    fun `만료된 R2 리뷰와 프로필 주소에서 원래 사진 경로를 찾는다`() {
        for (bucket in listOf("shop-review-images", "profile-avatars")) {
            assertEquals(SignedImageRequest(bucket, IMAGE_PATH), parser.parseExpired(r2Url(bucket), now))
        }
    }

    @Test
    fun `만료된 Supabase 호환 주소도 원래 사진 경로를 찾는다`() {
        for (bucket in listOf("shop-review-images", "profile-avatars")) {
            assertEquals(SignedImageRequest(bucket, IMAGE_PATH), parser.parseExpired(legacyUrl(bucket, now.epochSeconds), now))
        }
    }

    @Test
    fun `아직 유효한 주소는 재발급 대상으로 보지 않는다`() {
        assertNull(parser.parseExpired(r2Url().replace("20261103T120000Z", "20261103T120001Z"), now))
        assertNull(parser.parseExpired(legacyUrl("profile-avatars", now.epochSeconds + 1), now))
    }

    @Test
    fun `다른 계정이나 버킷과 공개 사진 및 업로드 주소는 무시한다`() {
        val urls =
            listOf(
                r2Url().replace(ACCOUNT_ID, "b".repeat(32)),
                r2Url().replace(PRIVATE_BUCKET, "other-private"),
                r2Url("shop-profile-images"),
                r2Url("pending"),
                r2Url().replace("https://", "http://"),
                r2Url().replace(".com/", ".com:8443/"),
                r2Url().replace(".com/", ".com.attacker.invalid/"),
                r2Url().replace("https://", "https://user:password@"),
                r2Url() + "#unexpected",
                legacyUrl("profile-avatars", now.epochSeconds).replace("fixture.invalid", "other.invalid"),
                "https://ramap-images.fixture.workers.dev/shop-menu-images/$IMAGE_PATH",
            )
        for (url in urls) assertNull(parser.parseExpired(url, now), url)
    }

    @Test
    fun `경로 조작이나 서명 만료 정보가 잘못된 주소는 무시한다`() {
        val urls =
            listOf(
                r2Url().replace(IMAGE_PATH, "../$IMAGE_PATH"),
                r2Url().replace("X-Amz-Signature=signature", "X-Amz-Signature="),
                r2Url().replace("20261103T120000Z", "invalid"),
                r2Url().replace("X-Amz-Expires=300", "X-Amz-Expires=-1"),
                r2Url().replace("X-Amz-Expires=300", "X-Amz-Expires=9999999999999999999"),
                r2Url() + "&X-Amz-Expires=300",
                legacyUrl("profile-avatars", now.epochSeconds).substringBefore("token=") + "token=bad-token",
                legacyUrl("profile-avatars", now.epochSeconds) + "&token=other-token",
            )
        for (url in urls) assertNull(parser.parseExpired(url, now), url)
    }

    private fun r2Url(bucket: String = "shop-review-images"): String = "https://$ACCOUNT_ID.r2.cloudflarestorage.com/$PRIVATE_BUCKET/$bucket/$IMAGE_PATH?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20261103T120000Z&X-Amz-Expires=300&X-Amz-Signature=signature"

    private fun legacyUrl(
        bucket: String,
        expirySeconds: Long,
    ): String {
        val payload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode("""{"exp":$expirySeconds}""".encodeToByteArray())
        return "$SUPABASE_BASE/$bucket/$IMAGE_PATH?token=header.$payload.signature"
    }

    private companion object {
        const val SUPABASE_BASE = "https://fixture.invalid/storage/v1/object/sign"
        const val ACCOUNT_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val PRIVATE_BUCKET = "ramap-media-private"
        const val IMAGE_PATH = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002.jpg"
    }
}
