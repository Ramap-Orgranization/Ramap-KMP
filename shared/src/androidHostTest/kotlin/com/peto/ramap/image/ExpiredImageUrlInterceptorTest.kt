package com.peto.ramap.image

import coil3.ColorImage
import coil3.ImageLoader
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.network.NetworkResponse
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import com.peto.ramap.domain.repository.ImageUrlRefresher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class ExpiredImageUrlInterceptorTest {
    @Test
    fun `만료 응답 뒤 새 주소로 사진을 불러오면 성공만 전달한다`() =
        runTest {
            for (code in listOf(400, 401, 403)) {
                val downloads = mutableListOf<String>()
                val refreshes = mutableListOf<String>()
                val refresher =
                    ImageUrlRefresher { url ->
                        refreshes += url
                        REFRESHED_URL
                    }
                val result =
                    load(refresher) { request ->
                        downloads += request.data as String
                        if (request.data == ORIGINAL_URL) expired(request, code) else SuccessResult(ColorImage(), request)
                    }

                assertIs<SuccessResult>(result)
                assertEquals(REFRESHED_URL, result.request.data)
                assertEquals(listOf(ORIGINAL_URL, REFRESHED_URL), downloads)
                assertEquals(listOf(ORIGINAL_URL), refreshes)
            }
        }

    @Test
    fun `새 주소도 실패하면 두 번째 결과로 종료해 재발급을 반복하지 않는다`() =
        runTest {
            val downloads = mutableListOf<String>()
            var refreshCount = 0
            val result =
                load(
                    ImageUrlRefresher {
                        refreshCount++
                        REFRESHED_URL
                    },
                ) { request ->
                    downloads += request.data as String
                    expired(request, 403)
                }

            assertIs<ErrorResult>(result)
            assertEquals(REFRESHED_URL, result.request.data)
            assertEquals(listOf(ORIGINAL_URL, REFRESHED_URL), downloads)
            assertEquals(1, refreshCount)
        }

    @Test
    fun `정상 로딩과 네트워크 및 서버 오류에서는 재발급하지 않는다`() =
        runTest {
            val refresher = ImageUrlRefresher { error("만료 응답에서만 재발급해야 합니다") }
            assertIs<SuccessResult>(load(refresher) { request -> SuccessResult(ColorImage(), request) })
            assertIs<ErrorResult>(load(refresher) { request -> ErrorResult(null, request, IllegalStateException("network")) })
            assertIs<ErrorResult>(load(refresher) { request -> expired(request, 500) })
        }

    @Test
    fun `권한 거절이나 미갱신 및 재발급 실패는 원래 오류로 마무리한다`() =
        runTest {
            val refreshers =
                listOf(
                    ImageUrlRefresher { null },
                    ImageUrlRefresher { ORIGINAL_URL },
                    ImageUrlRefresher { throw IllegalStateException("발급 실패") },
                )
            for (refresher in refreshers) {
                val errors = mutableListOf<ErrorResult>()
                val result =
                    load(refresher) { request ->
                        expired(request, 403).also(errors::add)
                    }
                assertEquals(1, errors.size)
                assertSame(errors.single(), result)
            }
        }

    @Test
    fun `화면 이탈로 재발급이 취소되면 다운로드를 재시도하지 않는다`() =
        runTest {
            var downloadCount = 0
            assertFailsWith<CancellationException> {
                load({ throw CancellationException("Cancelled") }) { request ->
                    downloadCount++
                    expired(request, 403)
                }
            }
            assertEquals(1, downloadCount)
        }

    private suspend fun load(
        refresher: ImageUrlRefresher,
        response: suspend (ImageRequest) -> ImageResult,
    ): ImageResult {
        val context = RuntimeEnvironment.getApplication()
        val loader =
            ImageLoader
                .Builder(context)
                .components {
                    add(ExpiredImageUrlInterceptor(refresher))
                    add(Interceptor { chain -> response(chain.request) })
                }.build()
        return try {
            loader.execute(
                ImageRequest
                    .Builder(context)
                    .data(ORIGINAL_URL)
                    .size(100, 100)
                    .build(),
            )
        } finally {
            loader.shutdown()
        }
    }

    private fun expired(
        request: ImageRequest,
        code: Int,
    ): ErrorResult = ErrorResult(null, request, HttpException(NetworkResponse(code = code)))

    private companion object {
        const val ORIGINAL_URL = "https://fixture.invalid/expired.jpg"
        const val REFRESHED_URL = "https://fixture.invalid/refreshed.jpg"
    }
}
