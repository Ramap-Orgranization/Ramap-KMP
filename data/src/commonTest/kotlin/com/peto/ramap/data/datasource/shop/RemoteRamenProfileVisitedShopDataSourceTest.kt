package com.peto.ramap.data.datasource.shop

import com.peto.ramap.fixture.authenticatedSessionFixture
import com.peto.ramap.fixture.ramenShopResponseFixture
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(SupabaseInternal::class)
class RemoteRamenProfileVisitedShopDataSourceTest {
    @Test
    fun `저장한 매장이 많아도 요청을 나누어 모든 매장을 조회한다`() =
        runTest {
            val shopIds = (1..256).map { "00000000-0000-0000-0000-${it.toString().padStart(12, '0')}" }
            val requestedBatchSizes = mutableListOf<Int>()
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Auth) {
                        minimalConfig()
                        autoSetupPlatform = false
                    }
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            assertEquals("/rest/v1/ramen_shops", request.url.encodedPath)
                            val idFilter = requireNotNull(request.url.parameters["id"])
                            val batch = idFilter.removePrefix("in.(").removeSuffix(")").split(',')
                            requestedBatchSizes += batch.size
                            respond(
                                Json.encodeToString(batch.map { ramenShopResponseFixture(id = it) }),
                                headers = headersOf("Content-Type", "application/json"),
                            )
                        }
                }
            try {
                client.auth.setSessionStatus(authenticatedSessionFixture())
                val shops = RemoteRamenShopDataSource(client).fetchRamenShopsByIds(shopIds.toSet())

                assertEquals(listOf(100, 100, 56), requestedBatchSizes)
                assertEquals(shopIds, shops.map { it.id })
            } finally {
                client.close()
            }
        }
}
