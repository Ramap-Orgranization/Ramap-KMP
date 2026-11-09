package com.peto.ramap.data.datasource.announcement

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.repository.DefaultAnnouncementRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Instant

class RemoteAnnouncementDataSourceTest {
    @Test
    fun `공지를 최신순으로 끝까지 조회하고 마크다운과 게시 시각을 보존한다`() =
        runTest {
            var page = 0
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine { request ->
                            assertEquals("/rest/v1/service_announcements", request.url.encodedPath)
                            assertEquals("published_at.desc.nullslast,id.desc.nullslast", request.url.parameters["order"])
                            assertEquals((page * 100).toString(), request.url.parameters["offset"])
                            assertEquals("100", request.url.parameters["limit"])
                            val count = if (page++ == 0) 100 else 1
                            val content =
                                (0 until count).joinToString(prefix = "[", postfix = "]") { index ->
                                    """{"id":"$page-$index","title":"공지","body_markdown":"# 제목\n**강조**","published_at":"2026-11-09T09:00:00+09:00"}"""
                                }
                            respond(content, headers = headersOf("Content-Type", "application/json"))
                        }
                    install(Postgrest)
                }
            try {
                val result = DefaultAnnouncementRepository(RemoteAnnouncementDataSource(client)).fetchAnnouncements().getOrThrow()
                assertEquals(101, result.size)
                assertEquals("# 제목\n**강조**", result.first().bodyMarkdown)
                assertEquals(Instant.parse("2026-11-09T00:00:00Z"), result.first().publishedAt)
            } finally {
                client.close()
            }
        }

    @Test
    fun `서버 조회 실패를 빈 공지 목록으로 바꾸지 않는다`() =
        runTest {
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine {
                            respond("""{"message":"unavailable"}""", HttpStatusCode.ServiceUnavailable, headersOf("Content-Type", "application/json"))
                        }
                    install(Postgrest)
                }
            try {
                assertIs<RamapResult.Error>(DefaultAnnouncementRepository(RemoteAnnouncementDataSource(client)).fetchAnnouncements())
            } finally {
                client.close()
            }
        }

    @Test
    fun `조회 취소를 오류 결과로 삼키지 않는다`() =
        runTest {
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine = MockEngine { throw CancellationException("cancelled") }
                    install(Postgrest)
                }
            try {
                assertFailsWith<CancellationException> {
                    DefaultAnnouncementRepository(RemoteAnnouncementDataSource(client)).fetchAnnouncements()
                }
            } finally {
                client.close()
            }
        }
}
