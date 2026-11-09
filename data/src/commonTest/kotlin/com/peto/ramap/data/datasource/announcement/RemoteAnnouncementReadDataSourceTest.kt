package com.peto.ramap.data.datasource.announcement

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RemoteAnnouncementReadDataSourceTest {
    @Test
    fun `사용자 필터와 정렬을 고정하여 읽음 기록을 끝까지 가져온다`() =
        runTest {
            var page = 0
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine { request ->
                            assertEquals("/rest/v1/service_announcement_reads", request.url.encodedPath)
                            assertEquals("eq.owner", request.url.parameters["user_id"])
                            assertEquals("announcement_id.asc.nullslast", request.url.parameters["order"])
                            assertEquals((page * 100).toString(), request.url.parameters["offset"])
                            assertEquals("100", request.url.parameters["limit"])
                            val count = if (page++ == 0) 100 else 1
                            val body =
                                (0 until count).joinToString(prefix = "[", postfix = "]") {
                                    """{"user_id":"owner","announcement_id":"$page-$it"}"""
                                }
                            respond(body, headers = headersOf("Content-Type", "application/json"))
                        }
                    install(Postgrest)
                }
            try {
                assertEquals(101, RemoteAnnouncementReadDataSource(client).fetchReadIds("owner").size)
            } finally {
                client.close()
            }
        }

    @Test
    fun `읽음 등록은 요청 계정을 명시하고 중복과 기존 서버 시각을 보존한다`() =
        runTest {
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine { request ->
                            assertEquals(HttpMethod.Post, request.method)
                            assertEquals("user_id,announcement_id", request.url.parameters["on_conflict"])
                            assertTrue(request.headers["Prefer"].orEmpty().contains("resolution=ignore-duplicates"))
                            val body =
                                Json
                                    .parseToJsonElement(request.body.toByteArray().decodeToString())
                                    .jsonArray
                                    .single()
                                    .jsonObject
                            assertEquals("owner", body["user_id"]?.jsonPrimitive?.content)
                            assertEquals("notice", body["announcement_id"]?.jsonPrimitive?.content)
                            assertFalse("read_at" in body)
                            respond("", headers = headersOf("Content-Type", "application/json"))
                        }
                    install(Postgrest)
                }
            try {
                RemoteAnnouncementReadDataSource(client).markRead("notice", "owner")
            } finally {
                client.close()
            }
        }
}
