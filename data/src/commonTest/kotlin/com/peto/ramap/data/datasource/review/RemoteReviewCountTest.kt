package com.peto.ramap.data.datasource.review

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import com.peto.ramap.data.repository.DefaultReviewRepository
import com.peto.ramap.data.testReviewChanges
import com.peto.ramap.fake.FakeRamenShopDataSource
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteReviewCountTest {
    @Test
    fun `내 리뷰 개수는 목록과 사진 발급 및 매장 조회 없이 숫자만 읽는다`() =
        runTest {
            val requests = mutableListOf<String>()
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            requests += request.url.encodedPath
                            check(request.url.encodedPath == "/rest/v1/rpc/fetch_my_review_count") { "리뷰 개수 API만 호출해야 합니다" }
                            respond("7", headers = headersOf("Content-Type", "application/json"))
                        }
                }
            val images =
                object : ImageUrlDataSource {
                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = error("개수 조회 중 사진 주소를 요청했습니다")
                }
            try {
                val source = RemoteReviewDataSource(client, ReviewImageResolver(images))
                val shops = FakeRamenShopDataSource()
                val repository = DefaultReviewRepository(source, shops, testReviewChanges(), backgroundScope)

                assertEquals(7, repository.fetchMyReviewCount().getOrThrow())
                assertEquals(listOf("/rest/v1/rpc/fetch_my_review_count"), requests)
                assertNull(shops.requestedShopIds)
            } finally {
                client.close()
            }
        }
}
