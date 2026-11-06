package com.peto.ramap.data.datasource.image

import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.network.client.media.MediaStorageClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(SupabaseInternal::class)
class MediaStorageClientTest {
    @Test
    fun `사진 바이트는 R2에 직접 보내고 로그인 토큰은 전달하지 않는다`() =
        runTest {
            val calls = mutableListOf<HttpRequestData>()
            val id = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
            val engine =
                MockEngine { request ->
                    calls += request
                    when (request.url.encodedPath) {
                        "/v2/prepare" -> respond("""{"provider":"r2","id":"$id","ready":false,"url":"https://${R2_ACCOUNT}.r2.cloudflarestorage.com/r2-private/pending/$id?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed"}""", headers = JSON_HEADERS)
                        "/r2-private/pending/$id" -> {
                            assertNull(request.headers["Authorization"])
                            assertNull(request.headers["apikey"])
                            assertEquals("*", request.headers["If-None-Match"])
                            respond("", HttpStatusCode.OK)
                        }
                        "/v2/finalize" -> respond("""{"ready":true}""", headers = JSON_HEADERS)
                        else -> error("예상하지 않은 요청")
                    }
                }
            withClient(engine) { client, media ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                media.upload("shop-review-images", "viewer/photo.jpg", byteArrayOf(1, 2, 3), "image/jpeg")
                assertEquals(3, calls.size)
                assertEquals("Bearer test-access-token", calls.first().headers["Authorization"])
                assertEquals("Bearer test-access-token", calls.last().headers["Authorization"])
            }
        }

    @Test
    fun `신뢰하지 않는 업로드 호스트에는 사진을 보내지 않는다`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respond("""{"provider":"r2","id":"upload","ready":false,"url":"https://other.invalid/private/pending/upload?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed"}""", headers = JSON_HEADERS)
                }
            withClient(engine) { _, media ->
                assertFailsWith<IllegalStateException> { media.upload("shop-review-images", "viewer/photo.jpg", byteArrayOf(1), "image/jpeg") }
                assertEquals(1, calls)
            }
        }

    @Test
    fun `서명이 있어도 다른 객체 경로의 URL은 재사용하지 않는다`() =
        runTest {
            val expiry = Clock.System.now() + 5.minutes
            val engine =
                MockEngine {
                    respond("""[{"bucket":"profile-avatars","path":"viewer/photo.jpg","url":"https://${R2_ACCOUNT}.r2.cloudflarestorage.com/r2-private/profile-avatars/other/photo.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed","expires_at":"$expiry"}]""", headers = JSON_HEADERS)
                }
            withClient(engine) { _, media ->
                assertTrue(media.fetchUrls("profile-avatars", listOf("viewer/photo.jpg")).isEmpty())
            }
        }

    @Test
    fun `권한 API 실패를 이전 저장소 업로드로 우회하지 않는다`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respond("{}", HttpStatusCode.Forbidden, JSON_HEADERS)
                }
            withClient(engine) { _, media ->
                assertFailsWith<IllegalStateException> { media.upload("shop-review-images", "viewer/photo.jpg", byteArrayOf(1), "image/jpeg") }
                assertEquals(1, calls)
            }
        }

    @Test
    fun `R2 사진 전송은 계정과 버킷을 확인하고 인증 정보를 저장소에 보내지 않는다`() =
        runTest {
            val id = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
            var uploads = 0
            val engine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/v2/prepare" -> respond("""{"provider":"r2","id":"$id","ready":false,"url":"https://${R2_ACCOUNT}.r2.cloudflarestorage.com/r2-private/pending/$id?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed"}""", headers = JSON_HEADERS)
                        "/r2-private/pending/$id" -> {
                            uploads++
                            assertNull(request.headers["Authorization"])
                            assertNull(request.headers["apikey"])
                            assertEquals("*", request.headers["If-None-Match"])
                            respond("", HttpStatusCode.OK)
                        }
                        "/v2/finalize" -> respond("""{"ready":true}""", headers = JSON_HEADERS)
                        else -> error("예상하지 않은 요청")
                    }
                }
            withClient(engine) { _, media ->
                media.upload("shop-review-images", "viewer/photo.jpg", byteArrayOf(1, 2, 3), "image/jpeg")
                assertEquals(1, uploads)
            }
        }

    @Test
    fun `R2 서명 URL도 다른 계정 버킷과 객체 경로이면 거절한다`() =
        runTest {
            val expiry = Clock.System.now() + 5.minutes
            val signature = "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=signed"
            val urls =
                listOf(
                    "https://${"b".repeat(32)}.r2.cloudflarestorage.com/r2-private/profile-avatars/viewer/photo.jpg?$signature",
                    "https://${R2_ACCOUNT}.r2.cloudflarestorage.com/other/profile-avatars/viewer/photo.jpg?$signature",
                    "https://${R2_ACCOUNT}.r2.cloudflarestorage.com/r2-private/profile-avatars/other/photo.jpg?$signature",
                    "https://${R2_ACCOUNT}.r2.cloudflarestorage.com/r2-private/profile-avatars/viewer/photo.jpg?X-Amz-Signature=signed",
                )
            for (url in urls) {
                val engine = MockEngine { respond("""[{"bucket":"profile-avatars","path":"viewer/photo.jpg","url":"$url","expires_at":"$expiry"}]""", headers = JSON_HEADERS) }
                withClient(engine) { _, media ->
                    assertTrue(media.fetchUrls("profile-avatars", listOf("viewer/photo.jpg")).isEmpty(), url)
                }
            }
        }

    private suspend fun withClient(
        engine: MockEngine,
        block: suspend (SupabaseClient, MediaStorageClient) -> Unit,
    ) {
        val supabase =
            createSupabaseClient("https://supabase.invalid", "anon") {
                httpEngine = MockEngine { error("사진 경로가 Supabase로 우회됨") }
                install(Storage)
                install(Auth) {
                    minimalConfig()
                    autoSetupPlatform = false
                }
            }
        val http = HttpClient(engine) { followRedirects = false }
        supabase.auth.setSessionStatus(SessionStatus.NotAuthenticated())
        try {
            block(supabase, MediaStorageClient(supabase, http, "https://media.invalid", R2_ACCOUNT, "r2-private"))
        } finally {
            http.close()
            supabase.close()
        }
    }

    private companion object {
        val R2_ACCOUNT = "a".repeat(32)
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
    }
}
