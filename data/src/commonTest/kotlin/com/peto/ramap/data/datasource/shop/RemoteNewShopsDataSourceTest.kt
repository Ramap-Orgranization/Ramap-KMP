package com.peto.ramap.data.datasource.shop

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.data.repository.DefaultNewShopsRepository
import com.peto.ramap.domain.model.shop.ShopOpening
import com.peto.ramap.domain.model.shop.ShopRegistrationMonth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class RemoteNewShopsDataSourceTest {
    @Test
    fun `신매장 조회에 등록월을 전달하고 같은 등록 시각의 다음 페이지는 ID로 이어간다`() =
        runTest {
            val registeredAt = "2026-11-09T23:31:23.956958+00:00"
            val openingDate = LocalDate(2026, 10, 28)
            val photo = "https://fixture.invalid/yugi.jpg"
            val firstPage =
                (101 downTo 2).map { index ->
                    NewShopResponse(
                        id = "shop-$index",
                        name = "매장 $index",
                        createdAt = registeredAt,
                        openingDate = openingDate,
                        openingKind = "soft".takeIf { index == 101 },
                        coverImageUrl = photo.takeIf { index == 101 },
                    )
                }
            val last =
                NewShopResponse(
                    id = "shop-1",
                    name = "매장 1",
                    createdAt = registeredAt,
                    openingDate = openingDate,
                    coverImageUrl = "http://fixture.invalid/test.jpg",
                )
            var requests = 0
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            assertEquals("/rest/v1/confirmed_new_shops", request.url.encodedPath)
                            val query = request.url.parameters
                            assertEquals("id,name,created_at,opening_date,opening_kind,cover_image_url", query["select"])
                            assertNull(query["is_visible"])
                            assertNull(query["opening_date"])
                            assertEquals(
                                "(created_at.gte.\"2026-10-31T15:00:00Z\",created_at.lt.\"2026-11-30T15:00:00Z\")",
                                query["and"],
                            )
                            assertEquals("created_at.desc.nullslast,id.desc.nullslast", query["order"])
                            assertEquals("100", query["limit"])
                            val page =
                                when (++requests) {
                                    1 -> {
                                        assertNull(query["or"])
                                        firstPage
                                    }

                                    2 -> {
                                        assertEquals(
                                            "(created_at.lt.\"$registeredAt\",and(created_at.eq.\"$registeredAt\",id.lt.shop-2))",
                                            query["or"],
                                        )
                                        listOf(last)
                                    }

                                    else -> error("마지막 페이지 뒤에 조회가 이어졌습니다.")
                                }
                            respond(Json.encodeToString(page), headers = headersOf("Content-Type", "application/json"))
                        }
                }

            try {
                val repository = DefaultNewShopsRepository(RemoteNewShopsDataSource(client))
                val month = ShopRegistrationMonth.containing(Instant.parse("2026-11-14T00:00:00Z"))
                val shops = repository.fetchNewShops(month).getOrThrow()

                assertEquals((101 downTo 1).map { "shop-$it" }, shops.map { it.id })
                assertEquals(ShopOpening(openingDate, ShopOpening.Kind.SOFT), shops.first().opening)
                assertEquals(photo, shops.first().coverImageUrl)
                assertEquals(ShopOpening(openingDate, ShopOpening.Kind.OPENING), shops.last().opening)
                assertNull(shops.last().coverImageUrl)
            } finally {
                client.close()
            }
        }
}
