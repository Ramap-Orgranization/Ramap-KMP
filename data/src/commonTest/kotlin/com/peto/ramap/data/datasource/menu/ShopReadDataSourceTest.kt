package com.peto.ramap.data.datasource.menu

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.shop.RemoteRamenShopDataSource
import com.peto.ramap.data.repository.DefaultShopMenuRepository
import com.peto.ramap.domain.model.shop.WaitingProvider
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ShopReadDataSourceTest {
    @Test
    fun `메뉴 조회는 상세 RPC 없이 메뉴 RPC만 호출하고 메뉴 필드를 읽는다`() =
        runTest {
            val paths = mutableListOf<String>()
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine { request ->
                            paths += request.url.encodedPath
                            val parameters = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
                            assertEquals("shop", parameters.getValue("p_shop_id").jsonPrimitive.content)
                            respond(
                                content = """[{"menu_sections":[{"id":"section","shop_id":"shop","title":"상시메뉴","display_order":0}],"menu_items":[{"id":"menu","section_id":"section","name":"쇼유라멘","display_order":0,"price_krw":10000}],"menu_updated_at":"2026-10-14T00:00:00Z"}]""",
                                headers = headersOf("Content-Type", "application/json"),
                            )
                        }
                    install(Postgrest)
                }
            try {
                val menu = DefaultShopMenuRepository(RemoteShopMenuDataSource(client)).fetchShopMenu("shop").getOrThrow()

                assertEquals(listOf("/rest/v1/rpc/fetch_shop_menu"), paths)
                assertEquals(
                    "쇼유라멘",
                    menu.sections
                        .single()
                        .items
                        .single()
                        .name,
                )
                assertEquals(1, menu.itemCount)
                assertEquals("2026-10-14T00:00:00Z", menu.updatedAt)
            } finally {
                client.close()
            }
        }

    @Test
    fun `상세 조회는 메뉴 필드가 없는 v3 RPC에서 매장 웨이팅을 읽는다`() =
        runTest {
            val paths = mutableListOf<String>()
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine =
                        MockEngine { request ->
                            paths += request.url.encodedPath
                            respond(
                                content = """[{"shop":{"id":"shop","name":"라멘집","address":"서울","lat":37.5,"lng":127.0,"created_at":"2026-06-01T00:00:00Z","updated_at":"2026-06-02T00:00:00Z","waiting_provider":"catchtable","waiting_provider_url":"https://example.com/waiting"},"like_count":7,"events":[],"event_participants":[],"operating_notice":null}]""",
                                headers = headersOf("Content-Type", "application/json"),
                            )
                        }
                    install(Postgrest)
                }
            try {
                val detail = RemoteRamenShopDataSource(client).fetchShopDetail("shop")

                assertEquals(listOf("/rest/v1/rpc/fetch_shop_detail_v3"), paths)
                assertEquals(7L, detail.likeCount)
                assertEquals(WaitingProvider.CATCHTABLE, detail.shop.toWaitingSystem()?.provider)
                assertEquals("https://example.com/waiting", detail.shop.toWaitingSystem()?.providerUrl)
            } finally {
                client.close()
            }
        }

    @Test
    fun `메뉴 RPC에 조회 가능한 매장이 없으면 빈 메뉴 성공으로 취급하지 않는다`() =
        runTest {
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    httpEngine = MockEngine { respond("[]", headers = headersOf("Content-Type", "application/json")) }
                    install(Postgrest)
                }
            try {
                val result = DefaultShopMenuRepository(RemoteShopMenuDataSource(client)).fetchShopMenu("missing")

                assertIs<RamapResult.Error>(result)
            } finally {
                client.close()
            }
        }
}
