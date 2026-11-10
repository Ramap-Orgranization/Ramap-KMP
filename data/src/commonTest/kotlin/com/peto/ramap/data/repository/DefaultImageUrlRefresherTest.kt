package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.image.CachedImageUrlDataSource
import com.peto.ramap.data.datasource.image.ExpiredImageUrlParser
import com.peto.ramap.data.datasource.image.ImageUrlAccessPolicy
import com.peto.ramap.data.datasource.image.ImageUrlAccessSnapshot
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

class DefaultImageUrlRefresherTest {
    private val parser = ExpiredImageUrlParser("https://fixture.invalid/storage/v1/object/sign", ACCOUNT_ID, "ramap-media-private")

    @Test
    fun `동시에 실패한 같은 사진은 새 주소 하나를 공유한다`() =
        runTest {
            val requests = mutableListOf<List<SignedImageRequest>>()
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        requests += images
                        delay(10)
                        return images.associateWith { SignedImageUrl(NEW_URL, Clock.System.now() + 5.minutes) }
                    }
                }
            val urls = CachedImageUrlDataSource(remote, accessPolicy())
            val refresher = DefaultImageUrlRefresher(parser, urls)

            val results = List(4) { async { refresher.refreshExpiredUrl(EXPIRED_URL) } }.awaitAll()

            assertEquals(List(4) { NEW_URL }, results)
            assertEquals(listOf(listOf(IMAGE)), requests)
        }

    @Test
    fun `권한 확인에서 거절된 사진은 새 주소를 반환하지 않는다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = images.associateWith { null }
                }
            assertNull(DefaultImageUrlRefresher(parser, remote).refreshExpiredUrl(EXPIRED_URL))
        }

    @Test
    fun `공개 주소와 만료 전 주소는 재발급을 요청하지 않는다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = error("재발급 대상이 아닙니다")
                }
            val refresher = DefaultImageUrlRefresher(parser, remote)
            assertNull(refresher.refreshExpiredUrl(NEW_URL))
            assertNull(refresher.refreshExpiredUrl(EXPIRED_URL.replace("20000101T000000Z", "20990101T000000Z")))
        }

    @Test
    fun `주소 재발급 중 로그아웃하면 받은 주소를 버린다`() =
        runTest {
            var currentSnapshot = ImageUrlAccessSnapshot("viewer", 0, 0)
            val policy =
                object : ImageUrlAccessPolicy {
                    override fun snapshot(): ImageUrlAccessSnapshot = currentSnapshot

                    override fun invalidatedImages(
                        snapshot: ImageUrlAccessSnapshot,
                        images: Collection<SignedImageRequest>,
                    ): Set<SignedImageRequest> = if (snapshot.userId == currentSnapshot.userId) emptySet() else images.toSet()
                }
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        currentSnapshot = ImageUrlAccessSnapshot(null, 1, 0)
                        return images.associateWith { SignedImageUrl(NEW_URL, Clock.System.now() + 5.minutes) }
                    }
                }
            val refresher = DefaultImageUrlRefresher(parser, CachedImageUrlDataSource(remote, policy))
            assertFailsWith<IllegalStateException> { refresher.refreshExpiredUrl(EXPIRED_URL) }
        }

    @Test
    fun `재발급 취소를 실패로 삼키지 않는다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = throw CancellationException("Cancelled")
                }
            assertFailsWith<CancellationException> { DefaultImageUrlRefresher(parser, remote).refreshExpiredUrl(EXPIRED_URL) }
        }

    private fun accessPolicy(): ImageUrlAccessPolicy =
        object : ImageUrlAccessPolicy {
            override fun snapshot(): ImageUrlAccessSnapshot = ImageUrlAccessSnapshot("viewer", 0, 0)

            override fun invalidatedImages(
                snapshot: ImageUrlAccessSnapshot,
                images: Collection<SignedImageRequest>,
            ): Set<SignedImageRequest> = emptySet()
        }

    private companion object {
        const val ACCOUNT_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val IMAGE_PATH = "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-000000000002.jpg"
        val IMAGE = SignedImageRequest("profile-avatars", IMAGE_PATH)
        const val EXPIRED_URL = "https://$ACCOUNT_ID.r2.cloudflarestorage.com/ramap-media-private/profile-avatars/$IMAGE_PATH?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20000101T000000Z&X-Amz-Expires=300&X-Amz-Signature=signature"
        const val NEW_URL = "https://fixture.invalid/new-image.jpg"
    }
}
