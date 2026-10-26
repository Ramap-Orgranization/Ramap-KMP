package com.peto.ramap.data.datasource.di

import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.moderation.UserModerationDataSource
import com.peto.ramap.data.datasource.profile.UserProfileDataSource
import com.peto.ramap.data.model.SignedImageRequest
import com.peto.ramap.data.model.SignedImageUrl
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class UserDataSourceModuleTest {
    @Test
    fun `분리한 사용자 데이터 소스를 주입해도 기존 서버 경로와 요청 형식을 유지한다`() =
        runTest {
            val requests = mutableListOf<Pair<String, JsonObject>>()
            val client =
                createSupabaseClient("https://fixture.invalid", "fixture-key") {
                    install(Postgrest)
                    httpEngine =
                        MockEngine { request ->
                            val rpc = request.url.encodedPath.substringAfterLast('/')
                            val body = request.body.toByteArray().decodeToString()
                            requests += rpc to Json.parseToJsonElement(body.ifBlank { "{}" }).jsonObject
                            val response =
                                when (rpc) {
                                    "fetch_my_community_membership" -> """{"user_id":"viewer","nickname":"작성자"}"""
                                    "fetch_community_profile_access" -> """{"status":"unavailable"}"""
                                    "fetch_profile_saved_shops" -> """{"access":{"status":"unavailable"},"shops":[]}"""
                                    "fetch_blocked_community_profiles_v2" -> "[]"
                                    "report_community_content", "change_community_block" -> "null"
                                    else -> error("예상하지 않은 API: $rpc")
                                }
                            respond(response, headers = headersOf("Content-Type", "application/json"))
                        }
                }
            val application =
                koinApplication {
                    modules(
                        dataSourceModule,
                        module {
                            single<SupabaseClient> { client }
                            single<ImageUrlDataSource> {
                                object : ImageUrlDataSource {
                                    override suspend fun fetchUrls(images: List<SignedImageRequest>): Map<SignedImageRequest, SignedImageUrl?> = emptyMap()
                                }
                            }
                        },
                    )
                }
            try {
                val profiles = application.koin.get<UserProfileDataSource>()
                val moderation = application.koin.get<UserModerationDataSource>()
                assertEquals("viewer", profiles.fetchMyProfile().userId)
                profiles.fetchProfileAccess("author")
                profiles.fetchUserSavedShops("author", 20L)
                assertEquals(emptyList(), moderation.fetchBlockedUsers())
                moderation.report("review", "review-id", "spam", "신고 내용")
                moderation.changeBlock("author", true)

                val expected =
                    listOf(
                        "fetch_my_community_membership" to "{}",
                        "fetch_community_profile_access" to """{"p_user_id":"author"}""",
                        "fetch_profile_saved_shops" to """{"p_user_id":"author","p_offset":20}""",
                        "fetch_blocked_community_profiles_v2" to "{}",
                        "report_community_content" to """{"p_target_type":"review","p_target_id":"review-id","p_reason":"spam","p_details":"신고 내용"}""",
                        "change_community_block" to """{"p_user_id":"author","p_blocked":true}""",
                    )
                assertEquals(expected.map { (rpc, body) -> rpc to Json.parseToJsonElement(body).jsonObject }, requests)
            } finally {
                application.close()
                client.close()
            }
        }
}
