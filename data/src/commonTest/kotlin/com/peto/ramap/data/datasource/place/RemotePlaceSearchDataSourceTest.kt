package com.peto.ramap.data.datasource.place

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.PlaceSearchCenterRequest
import com.peto.ramap.data.model.PlaceSearchRequest
import com.peto.ramap.data.repository.DefaultPlaceSearchRepository
import com.peto.ramap.domain.model.place.PlaceSearchErrorCode
import com.peto.ramap.domain.model.place.PlaceSearchException
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
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
import kotlin.test.assertNull

class RemotePlaceSearchDataSourceTest {
    @Test
    fun `실제 SDK의 하루 제한 응답을 대기 시간을 가진 도메인 오류로 전달한다`() =
        runTest {
            var calls = 0
            val engine =
                MockEngine {
                    calls++
                    respond(
                        content = """{"error":"limited","code":"search_rate_limited","limit_window":"day","retry_after_seconds":300}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf("Content-Type" to listOf("application/json"), "Retry-After" to listOf("120")),
                    )
                }
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine = engine
                    install(Functions)
                }
            try {
                val repository = DefaultPlaceSearchRepository(RemotePlaceSearchDataSource(client))
                val result = repository.search(SearchQuery("테스트"), Location(37.0, 127.0))
                val error = assertIs<RamapResult.Error>(result).error
                val failure = assertIs<PlaceSearchException>(error.cause)
                assertEquals(PlaceSearchErrorCode.DAILY_LIMIT, failure.code)
                assertEquals(120L, failure.retryAfterSeconds)
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }

    @Test
    fun `분과 시간 제한은 일일 제한으로 추측하지 않는다`() {
        for (window in listOf("minute", "hour", "unknown")) {
            val error = PlaceSearchErrorParser.parse(429, """{"code":"search_rate_limited","limit_window":"$window","retry_after_seconds":3600}""", null)
            assertEquals(PlaceSearchErrorCode.REQUEST_LIMIT, error?.code)
            assertEquals(3600L, error?.retryAfterSeconds)
        }
    }

    @Test
    fun `이전 서버 응답도 일반 제한 안내로 처리한다`() {
        val error = PlaceSearchErrorParser.parse(429, """{"code":"search_rate_limited"}""", null)
        assertEquals(PlaceSearchErrorCode.REQUEST_LIMIT, error?.code)
        assertNull(error?.retryAfterSeconds)
    }

    @Test
    fun `잘못된 헤더는 유효한 본문 대기 시간으로 보완한다`() {
        for (header in listOf("-1", "0", "not-a-number", "99999999999999999999999", "86401")) {
            val error = PlaceSearchErrorParser.parse(429, """{"code":"search_rate_limited","retry_after_seconds":25}""", header)
            assertEquals(25L, error?.retryAfterSeconds)
        }
    }

    @Test
    fun `대기 시간이 잘못되면 숫자를 표시하지 않는다`() {
        val error = PlaceSearchErrorParser.parse(429, """{"code":"search_rate_limited","retry_after_seconds":-30}""", null)
        assertNull(error?.retryAfterSeconds)
        assertEquals(PlaceSearchErrorCode.REQUEST_LIMIT, error?.code)
    }

    @Test
    fun `503과 일반 오류를 검색 횟수 소진으로 표시하지 않는다`() {
        assertEquals(PlaceSearchErrorCode.UNAVAILABLE, PlaceSearchErrorParser.parse(503, "invalid-json", "3")?.code)
        assertNull(PlaceSearchErrorParser.parse(400, """{"code":"search_rate_limited"}""", "30"))
        assertNull(PlaceSearchErrorParser.parse(429, """{"code":"another_limit"}""", "30"))
    }

    @Test
    fun `검색 호출 취소는 안내 오류로 변환하거나 재시도하지 않는다`() =
        runTest {
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine = MockEngine { throw CancellationException("cancelled") }
                    install(Functions)
                }
            try {
                assertFailsWith<CancellationException> {
                    RemotePlaceSearchDataSource(client).search(PlaceSearchRequest("테스트", PlaceSearchCenterRequest(37.0, 127.0)))
                }
            } finally {
                client.close()
            }
        }
}
