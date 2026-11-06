package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.data.repository.DefaultImageUrlRefresher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class BatchingImageUrlDataSourceTest {
    @Test
    fun `동시에 만료된 서로 다른 사진을 묶고 같은 사진의 결과를 공유한다`() =
        runTest {
            val batches = mutableListOf<List<SignedImageRequest>>()
            val policy = FakeImageUrlAccessPolicy()
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        batches += images
                        return images.associateWith(::signed)
                    }
                }
            val batched = BatchingImageUrlDataSource(CachedImageUrlDataSource(remote, policy), policy, backgroundScope)
            val parser = ExpiredImageUrlParser("https://fixture.invalid/storage/v1/object/sign", ACCOUNT_ID, "private")
            val refresher = DefaultImageUrlRefresher(parser, batched)
            val images = (1..20).map(::image)
            val results = (images + images.first()).map { image -> async { refresher.refreshExpiredUrl(expired(image)) } }.awaitAll()

            assertEquals((images + images.first()).map { signed(it).url }, results)
            assertEquals(listOf(images), batches)
        }

    @Test
    fun `네트워크 응답을 기다리는 동안 같은 사진 요청이 와도 결과 하나를 공유한다`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var requests = 0
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        requests++
                        started.complete(Unit)
                        release.await()
                        return images.associateWith(::signed)
                    }
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            val first = async { batched.fetchUrls(listOf(image(1))) }
            started.await()
            val second = async { batched.fetchUrls(listOf(image(1))) }
            runCurrent()
            release.complete(Unit)

            assertEquals(mapOf(image(1) to signed(image(1))), first.await())
            assertEquals(first.await(), second.await())
            assertEquals(1, requests)
        }

    @Test
    fun `한 화면의 취소는 같은 사진을 기다리는 다른 화면을 취소하지 않는다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = images.associateWith(::signed)
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            val first = async { batched.fetchUrls(listOf(image(1))) }
            val second = async { batched.fetchUrls(listOf(image(1))) }
            runCurrent()
            first.cancelAndJoin()

            assertTrue(first.isCancelled)
            assertEquals(mapOf(image(1) to signed(image(1))), second.await())
        }

    @Test
    fun `수집 중 모든 요청이 취소되면 네트워크에 보내지 않는다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = error("취소된 요청이 전송됨")
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            val pending = async { batched.fetchUrls(listOf(image(1))) }
            runCurrent()
            pending.cancelAndJoin()
            advanceTimeBy(100)
            runCurrent()
            assertTrue(pending.isCancelled)
        }

    @Test
    fun `배치 일부가 거절되어도 허용된 사진의 결과는 보존한다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = images.associateWith { if (it == image(2)) null else signed(it) }
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            val results = (1..3).map { index -> async { batched.fetchUrls(listOf(image(index)))[image(index)] } }.awaitAll()

            assertEquals(listOf(signed(image(1)), null, signed(image(3))), results)
        }

    @Test
    fun `실패한 공유 요청은 제거되어 다음 호출에서 다시 발급할 수 있다`() =
        runTest {
            var fail = true
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        if (fail) error("일시 장애")
                        return images.associateWith(::signed)
                    }
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            val failures = List(2) { async { runCatching { batched.fetchUrls(listOf(image(1))) } } }.awaitAll()
            assertTrue(failures.all { it.exceptionOrNull() is IllegalStateException })
            fail = false
            assertEquals(mapOf(image(1) to signed(image(1))), batched.fetchUrls(listOf(image(1))))
        }

    @Test
    fun `수집 중 계정이 바뀌면 이전 요청은 버리고 새 계정 요청을 분리한다`() =
        runTest {
            val policy = FakeImageUrlAccessPolicy()
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = images.associateWith(::signed)
                }
            val batched = BatchingImageUrlDataSource(remote, policy, backgroundScope)
            val previous = async { runCatching { batched.fetchUrls(listOf(image(1))) } }
            runCurrent()
            policy.access = ImageUrlAccessSnapshot("other", 1, 0)
            val current = async { batched.fetchUrls(listOf(image(1))) }

            assertIs<IllegalStateException>(previous.await().exceptionOrNull())
            assertEquals(mapOf(image(1) to signed(image(1))), current.await())
        }

    @Test
    fun `응답 중 권한이 바뀌면 배치 결과를 전달하지 않는다`() =
        runTest {
            val policy = FakeImageUrlAccessPolicy()
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> {
                        delay(10)
                        policy.access = policy.access.copy(revision = 1)
                        return images.associateWith(::signed)
                    }
                }
            val batched = BatchingImageUrlDataSource(remote, policy, backgroundScope)
            assertFailsWith<IllegalStateException> { batched.fetchUrls(listOf(image(1))) }
        }

    @Test
    fun `네트워크 취소는 빈 주소로 바꾸지 않고 호출자에게 전파한다`() =
        runTest {
            val remote =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = throw CancellationException("취소")
                }
            val batched = BatchingImageUrlDataSource(remote, FakeImageUrlAccessPolicy(), backgroundScope)
            assertFailsWith<CancellationException> { batched.fetchUrls(listOf(image(1))) }
        }

    private fun image(index: Int): SignedImageRequest = SignedImageRequest("profile-avatars", "$USER_ID/00000000-0000-4000-8000-${index.toString().padStart(12, '0')}.jpg")

    private fun expired(image: SignedImageRequest): String = "https://$ACCOUNT_ID.r2.cloudflarestorage.com/private/${image.bucket}/${image.path}?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20000101T000000Z&X-Amz-Expires=300&X-Amz-Signature=signature"

    private fun signed(image: SignedImageRequest): SignedImageUrl = SignedImageUrl("https://fixture.invalid/${image.path}", EXPIRES_AT)

    private companion object {
        const val ACCOUNT_ID = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val USER_ID = "00000000-0000-4000-8000-000000000001"
        val EXPIRES_AT = Clock.System.now() + 5.minutes
    }
}
