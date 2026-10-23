package com.peto.ramap.data.datasource.shop

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.repository.DefaultRamenShopRepository
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteMapShopSearchDataSourceTest {
    @Test
    fun `지도 검색은 정규화한 원문과 기준 위치를 RPC로 보내고 서버 결과 순서를 유지한다`() =
        runTest {
            var calls = 0
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            calls++
                            assertEquals("/rest/v1/rpc/search_map_ramen_shops", request.url.encodedPath)
                            val parameters = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                            assertEquals("멘야 타마시", parameters.getValue("p_query").jsonPrimitive.content)
                            assertEquals("37.5", parameters.getValue("p_lat").jsonPrimitive.content)
                            assertEquals("127.0", parameters.getValue("p_lng").jsonPrimitive.content)
                            assertEquals("50", parameters.getValue("p_limit").jsonPrimitive.content)
                            respond(
                                """[{"id":"name","name":"멘야 타마시","address":"세종","lat":36.48,"lng":127.29,"created_at":"2026-10-22","updated_at":"2026-10-22"},{"id":"address","name":"다른 매장","address":"서울","lat":37.5,"lng":127.0,"created_at":"2026-10-22","updated_at":"2026-10-22"}]""",
                                headers = headersOf("Content-Type", "application/json"),
                            )
                        }
                }
            try {
                val search = SearchRamenShopsUseCase(DefaultRamenShopRepository(RemoteRamenShopDataSource(client)))
                val shops = search(SearchQuery("  멘야   타마시  "), Location(37.5, 127.0)).getOrThrow()
                assertEquals(listOf("name", "address"), shops.keys.toList())
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }

    @Test
    fun `위치가 없으면 좌표를 비워 보내고 유효하지 않은 검색어는 요청하지 않는다`() =
        runTest {
            var calls = 0
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            calls++
                            val parameters = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                            assertEquals(JsonNull, parameters.getValue("p_lat"))
                            assertEquals(JsonNull, parameters.getValue("p_lng"))
                            respond("[]", headers = headersOf("Content-Type", "application/json"))
                        }
                }
            try {
                val search = SearchRamenShopsUseCase(DefaultRamenShopRepository(RemoteRamenShopDataSource(client)))
                search(SearchQuery("세종")).getOrThrow()
                search(SearchQuery(" ")).getOrThrow()
                assertEquals(1, calls)
            } finally {
                client.close()
            }
        }
}
