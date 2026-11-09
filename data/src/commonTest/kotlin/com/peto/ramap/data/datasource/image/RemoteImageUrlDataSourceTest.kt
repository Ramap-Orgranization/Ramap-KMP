package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.network.client.media.MediaStorageClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@OptIn(SupabaseInternal::class)
class RemoteImageUrlDataSourceTest {
    @Test
    fun `중복 경로는 제거하고 버킷별 기본 배치 두 번만 호출한다`() =
        runTest {
            val calls = mutableListOf<HttpRequestData>()
            val engine =
                MockEngine { request ->
                    calls.add(request)
                    assertNull(request.headers["Authorization"])
                    assertNull(request.headers["apikey"])
                    respond(success(request), headers = JSON_HEADERS)
                }
            withClient(engine) { _, source ->
                val before = Clock.System.now()
                val avatar = image("profile-avatars", 1)
                val review = image("shop-review-images", 2)
                val result = source.fetchUrls(listOf(avatar, avatar, review))
                assertEquals(2, calls.size)
                assertEquals(2, result.size)
                result.values.forEach {
                    assertNotNull(it)
                    assertTrue(it.expiresAt > before + 4.minutes)
                    assertTrue(it.expiresAt <= Clock.System.now() + 5.minutes)
                }
                assertTrue(calls.all { it.url.encodedPath == "/v2/resolve" })
                assertEquals(setOf(avatar.bucket, review.bucket), calls.flatMap { images(it) }.map { it.bucket }.toSet())
            }
        }

    @Test
    fun `거부된 이미지의 null URL이 같은 배치의 성공 결과를 지우지 않는다`() =
        runTest {
            val allowed = image("profile-avatars", 1)
            val denied = image("profile-avatars", 2)
            val engine =
                MockEngine {
                    respond(
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("bucket", denied.bucket)
                                    put("path", denied.path)
                                    put("url", JsonNull)
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("bucket", allowed.bucket)
                                    put("path", allowed.path)
                                    put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                    put("url", signedPath(allowed))
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { _, source ->
                val result = source.fetchUrls(listOf(allowed, denied))
                assertNotNull(result[allowed])
                assertNull(result[denied])
            }
        }

    @Test
    fun `로그인한 사용자의 토큰을 사진 API에 전달한다`() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("Bearer test-access-token", request.headers["Authorization"])
                    respond(success(request), headers = JSON_HEADERS)
                }
            withClient(engine) { client, source ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                assertNotNull(source.fetchUrls(listOf(image("profile-avatars", 1))).values.single())
            }
        }

    @Test
    fun `버킷 요청이 거부되어도 다른 버킷의 결과는 유지한다`() =
        runTest {
            val engine =
                MockEngine { request ->
                    if (images(request).first().bucket == "profile-avatars") {
                        respond("""{"message":"Forbidden","statusCode":"403","error":"Forbidden"}""", HttpStatusCode.Forbidden, JSON_HEADERS)
                    } else {
                        respond(success(request), headers = JSON_HEADERS)
                    }
                }
            withClient(engine) { _, source ->
                val avatar = image("profile-avatars", 1)
                val review = image("shop-review-images", 2)
                val result = source.fetchUrls(listOf(avatar, review))
                assertNull(result[avatar])
                assertNotNull(result[review])
            }
        }

    @Test
    fun `중복 응답과 잘못된 URL 및 누락된 경로는 재사용하지 않는다`() =
        runTest {
            val requested = (1..4).map { image("profile-avatars", it) }
            val engine =
                MockEngine {
                    respond(
                        buildJsonArray {
                            repeat(2) {
                                add(
                                    buildJsonObject {
                                        put("bucket", requested[0].bucket)
                                        put("path", requested[0].path)
                                        put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                        put("url", signedPath(requested[0]))
                                    },
                                )
                            }
                            add(
                                buildJsonObject {
                                    put("bucket", requested[1].bucket)
                                    put("path", requested[1].path)
                                    put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                    put("url", signedPath(requested[2]))
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("bucket", requested[2].bucket)
                                    put("path", requested[2].path)
                                    put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                    put("url", "https://other.example/image")
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { _, source ->
                assertTrue(source.fetchUrls(requested).values.all { it == null })
            }
        }

    @Test
    fun `빈 입력과 지원하지 않는 경로는 네트워크를 호출하지 않는다`() =
        runTest {
            val engine = MockEngine { error("Unexpected request") }
            withClient(engine) { _, source ->
                assertTrue(source.fetchUrls(emptyList()).isEmpty())
                assertNull(source.fetchUrls(listOf(SignedImageRequest("other", "../file"))).values.single())
            }
        }

    @Test
    fun `큰 입력은 버킷 안에서 64개씩 나눠 호출한다`() =
        runTest {
            val sizes = mutableListOf<Int>()
            val lock = Mutex()
            var active = 0
            var peak = 0
            val engine =
                MockEngine { request ->
                    val size = images(request).size
                    lock.withLock {
                        sizes.add(size)
                        active++
                        peak = maxOf(peak, active)
                    }
                    delay(50)
                    val body = success(request)
                    lock.withLock { active-- }
                    respond(body, headers = JSON_HEADERS)
                }
            withClient(engine) { _, source ->
                val requests = (1..130).map { image("shop-review-images", it) }
                val result = source.fetchUrls(requests)
                assertEquals(listOf(2, 64, 64), sizes.sorted())
                assertEquals(2, peak)
                assertTrue(result.values.all { it != null })
            }
        }

    @Test
    fun `요청 중 계정이 변경되면 받은 링크를 반환하지 않는다`() =
        runTest {
            lateinit var activeClient: SupabaseClient
            val engine =
                MockEngine { request ->
                    activeClient.auth.setSessionStatus(authenticatedSessionFixture("other-viewer"))
                    respond(success(request), headers = JSON_HEADERS)
                }
            withClient(engine) { client, source ->
                activeClient = client
                client.auth.setSessionStatus(authenticatedSessionFixture())
                assertFailsWith<IllegalStateException> { source.fetchUrls(listOf(image("profile-avatars", 1))) }
            }
        }

    @Test
    fun `취소를 이미지 실패로 삼키지 않는다`() =
        runTest {
            val engine = MockEngine { throw CancellationException("cancelled") }
            withClient(engine) { _, source ->
                assertFailsWith<CancellationException> { source.fetchUrls(listOf(image("profile-avatars", 1))) }
            }
        }

    @Test
    fun `서명 토큰이 없거나 fragment가 있는 링크는 반환하지 않는다`() =
        runTest {
            val images = (1..2).map { image("profile-avatars", it) }
            val engine =
                MockEngine {
                    respond(
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("bucket", images[0].bucket)
                                    put("path", images[0].path)
                                    put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                    put("url", signedPath(images[0]).substringBefore("&X-Amz-Signature="))
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("bucket", images[1].bucket)
                                    put("path", images[1].path)
                                    put("expires_at", (Clock.System.now() + 5.minutes).toString())
                                    put("url", signedPath(images[1]) + "#unexpected")
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { _, source ->
                assertTrue(source.fetchUrls(images).values.all { it == null })
            }
        }

    private suspend fun withClient(
        engine: MockEngine,
        block: suspend (SupabaseClient, RemoteImageUrlDataSource) -> Unit,
    ) {
        val client =
            createSupabaseClient("https://example.com", "test-key") {
                httpEngine = MockEngine { error("Unexpected Supabase request") }
                install(Auth) {
                    minimalConfig()
                    autoSetupPlatform = false
                }
            }
        val http = HttpClient(engine)
        client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
        try {
            val media = MediaStorageClient(client, http, "https://media.invalid", R2_ACCOUNT, "private")
            block(client, RemoteImageUrlDataSource(client, media))
        } finally {
            http.close()
            client.close()
        }
    }

    private suspend fun images(request: HttpRequestData): List<SignedImageRequest> =
        Json
            .parseToJsonElement(request.body.toByteArray().decodeToString())
            .jsonObject
            .getValue("images")
            .jsonArray
            .map {
                val row = it.jsonObject
                SignedImageRequest(row.getValue("bucket").jsonPrimitive.content, row.getValue("path").jsonPrimitive.content)
            }

    private suspend fun success(request: HttpRequestData): String =
        buildJsonArray {
            images(request).reversed().forEach { image ->
                add(
                    buildJsonObject {
                        put("bucket", image.bucket)
                        put("path", image.path)
                        put("url", signedPath(image))
                        put("expires_at", (Clock.System.now() + 5.minutes).toString())
                    },
                )
            }
        }.toString()

    private fun image(
        bucket: String,
        index: Int,
    ) = SignedImageRequest(bucket, "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-${index.toString().padStart(12, '0')}.jpg")

    private fun signedPath(image: SignedImageRequest) = "https://$R2_ACCOUNT.r2.cloudflarestorage.com/private/${image.bucket}/${image.path}?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Signature=test-signature"

    private companion object {
        val R2_ACCOUNT = "a".repeat(32)
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
    }
}
