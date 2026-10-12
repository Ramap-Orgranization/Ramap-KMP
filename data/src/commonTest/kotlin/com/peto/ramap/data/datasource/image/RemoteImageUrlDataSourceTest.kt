package com.peto.ramap.data.datasource.image

import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.fixture.authenticatedSessionFixture
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
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
                    assertEquals("Bearer test-key", request.headers["Authorization"])
                    assertEquals("test-key", request.headers["apikey"])
                    respond(success(request), headers = JSON_HEADERS)
                }
            withClient(engine) { client ->
                val before = Clock.System.now()
                val avatar = image("profile-avatars", 1)
                val review = image("shop-review-images", 2)
                val result = RemoteImageUrlDataSource(client).fetchUrls(listOf(avatar, avatar, review))
                assertEquals(2, calls.size)
                assertEquals(2, result.size)
                result.values.forEach {
                    assertNotNull(it)
                    assertTrue(it.expiresAt > before + 4.minutes)
                    assertTrue(it.expiresAt <= Clock.System.now() + 5.minutes)
                }
                assertEquals(setOf("/storage/v1/object/sign/profile-avatars", "/storage/v1/object/sign/shop-review-images"), calls.map { it.url.encodedPath }.toSet())
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
                                    put("path", denied.path)
                                    put("signedURL", JsonNull)
                                    put("error", "Object not found")
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("path", allowed.path)
                                    put("signedURL", signedPath(allowed))
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { client ->
                val result = RemoteImageUrlDataSource(client).fetchUrls(listOf(allowed, denied))
                assertNotNull(result[allowed])
                assertNull(result[denied])
            }
        }

    @Test
    fun `로그인한 사용자의 토큰을 Storage에 전달한다`() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("Bearer test-access-token", request.headers["Authorization"])
                    respond(success(request), headers = JSON_HEADERS)
                }
            withClient(engine) { client ->
                client.auth.setSessionStatus(authenticatedSessionFixture())
                assertNotNull(RemoteImageUrlDataSource(client).fetchUrls(listOf(image("profile-avatars", 1))).values.single())
            }
        }

    @Test
    fun `버킷 요청이 거부되어도 다른 버킷의 결과는 유지한다`() =
        runTest {
            val engine =
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith("profile-avatars")) {
                        respond("""{"message":"Forbidden","statusCode":"403","error":"Forbidden"}""", HttpStatusCode.Forbidden, JSON_HEADERS)
                    } else {
                        respond(success(request), headers = JSON_HEADERS)
                    }
                }
            withClient(engine) { client ->
                val avatar = image("profile-avatars", 1)
                val review = image("shop-review-images", 2)
                val result = RemoteImageUrlDataSource(client).fetchUrls(listOf(avatar, review))
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
                                        put("path", requested[0].path)
                                        put("signedURL", signedPath(requested[0]))
                                    },
                                )
                            }
                            add(
                                buildJsonObject {
                                    put("path", requested[1].path)
                                    put("signedURL", signedPath(requested[2]))
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("path", requested[2].path)
                                    put("signedURL", "https://other.example/image")
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { client ->
                assertTrue(RemoteImageUrlDataSource(client).fetchUrls(requested).values.all { it == null })
            }
        }

    @Test
    fun `빈 입력과 지원하지 않는 경로는 네트워크를 호출하지 않는다`() =
        runTest {
            val engine = MockEngine { error("Unexpected request") }
            withClient(engine) { client ->
                val source = RemoteImageUrlDataSource(client)
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
                    val size = paths(request).size
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
            withClient(engine) { client ->
                val requests = (1..130).map { image("shop-review-images", it) }
                val result = RemoteImageUrlDataSource(client).fetchUrls(requests)
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
            withClient(engine) { client ->
                activeClient = client
                client.auth.setSessionStatus(authenticatedSessionFixture())
                assertFailsWith<IllegalStateException> { RemoteImageUrlDataSource(client).fetchUrls(listOf(image("profile-avatars", 1))) }
            }
        }

    @Test
    fun `취소를 이미지 실패로 삼키지 않는다`() =
        runTest {
            val engine = MockEngine { throw CancellationException("cancelled") }
            withClient(engine) { client ->
                assertFailsWith<CancellationException> { RemoteImageUrlDataSource(client).fetchUrls(listOf(image("profile-avatars", 1))) }
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
                                    put("path", images[0].path)
                                    put("signedURL", signedPath(images[0]).substringBefore("token="))
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("path", images[1].path)
                                    put("signedURL", signedPath(images[1]) + "#unexpected")
                                },
                            )
                        }.toString(),
                        headers = JSON_HEADERS,
                    )
                }
            withClient(engine) { client ->
                assertTrue(RemoteImageUrlDataSource(client).fetchUrls(images).values.all { it == null })
            }
        }

    private suspend fun withClient(
        engine: MockEngine,
        block: suspend (SupabaseClient) -> Unit,
    ) {
        val client =
            createSupabaseClient("https://example.com", "test-key") {
                httpEngine = engine
                install(Storage)
                install(Auth) {
                    minimalConfig()
                    autoSetupPlatform = false
                }
            }
        client.auth.setSessionStatus(SessionStatus.NotAuthenticated())
        try {
            block(client)
        } finally {
            client.close()
        }
    }

    private suspend fun paths(request: HttpRequestData): List<String> {
        val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        assertEquals("300", body.getValue("expiresIn").jsonPrimitive.content)
        return body.getValue("paths").jsonArray.map { it.jsonPrimitive.content }
    }

    private suspend fun success(request: HttpRequestData): String =
        buildJsonArray {
            val bucket = request.url.encodedPath.substringAfterLast('/')
            paths(request).reversed().forEach { path ->
                add(
                    buildJsonObject {
                        put("path", path)
                        put("signedURL", signedPath(SignedImageRequest(bucket, path)))
                    },
                )
            }
        }.toString()

    private fun image(
        bucket: String,
        index: Int,
    ) = SignedImageRequest(bucket, "00000000-0000-0000-0000-000000000001/00000000-0000-0000-0000-${index.toString().padStart(12, '0')}.jpg")

    private fun signedPath(image: SignedImageRequest) = "/object/sign/${image.bucket}/${image.path}?token=test-token"

    private companion object {
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
    }
}
