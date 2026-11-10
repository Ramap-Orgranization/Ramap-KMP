package com.peto.ramap.data.auth

import com.peto.ramap.domain.model.auth.SignupConsentDismissedException
import com.peto.ramap.domain.model.auth.SignupTerms
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SignupConsentSignInTest {
    @Test
    fun `기존 계정은 가입 동의 요청 없이 인증 세션을 발급받는다`() =
        runTest {
            val client =
                client(
                    MockEngine { request ->
                        assertEquals(TOKEN_PATH, request.url.encodedPath)
                        respond(SESSION_BODY, headers = JSON_HEADERS)
                    },
                )
            try {
                SignupConsentSignIn(client) { error("기존 계정에는 동의를 요청하면 안 됩니다.") }.signIn(credentials)
                assertNotNull(client.auth.currentSessionOrNull())
            } finally {
                client.close()
            }
        }

    @Test
    fun `신규 가입에 동의하면 본 버전을 기록한 뒤 같은 인증 정보로 가입한다`() =
        runTest {
            val steps = mutableListOf<String>()
            val tokenBodies = mutableListOf<String>()
            val client =
                client(
                    MockEngine { request ->
                        when (request.url.encodedPath) {
                            TOKEN_PATH -> {
                                tokenBodies += request.body.toByteArray().decodeToString()
                                steps += "인증"
                                if (tokenBodies.size == 1) {
                                    respond(CHALLENGE_RESPONSE, HttpStatusCode.UnprocessableEntity, JSON_HEADERS)
                                } else {
                                    respond(SESSION_BODY, headers = JSON_HEADERS)
                                }
                            }
                            ACCEPT_PATH -> {
                                steps += "동의 기록"
                                val expected =
                                    buildJsonObject {
                                        put("p_challenge_id", CHALLENGE_ID)
                                        put("p_version", "v1")
                                    }
                                assertEquals(expected, Json.parseToJsonElement(request.body.toByteArray().decodeToString()))
                                respond("true", headers = JSON_HEADERS)
                            }
                            else -> error("예상하지 않은 요청")
                        }
                    },
                )
            try {
                SignupConsentSignIn(client) { terms ->
                    assertEquals(SignupTerms("v1", "https://example.test/terms/v1"), terms)
                    assertNull(client.auth.currentSessionOrNull())
                    steps += "화면 동의"
                    true
                }.signIn(credentials)
                assertEquals(listOf("인증", "화면 동의", "동의 기록", "인증"), steps)
                assertEquals(tokenBodies.first(), tokenBodies.last())
                assertNotNull(client.auth.currentSessionOrNull())
            } finally {
                client.close()
            }
        }

    @Test
    fun `약관 화면을 닫으면 동의를 기록하거나 계정을 만들지 않는다`() =
        runTest {
            val paths = mutableListOf<String>()
            val client =
                client(
                    MockEngine { request ->
                        paths += request.url.encodedPath
                        respond(CHALLENGE_RESPONSE, HttpStatusCode.UnprocessableEntity, JSON_HEADERS)
                    },
                )
            try {
                assertFailsWith<SignupConsentDismissedException> {
                    SignupConsentSignIn(client) { false }.signIn(credentials)
                }
                assertEquals(listOf(TOKEN_PATH), paths)
                assertNull(client.auth.currentSessionOrNull())
            } finally {
                client.close()
            }
        }

    private val credentials: IDToken.Config.() -> Unit = {
        idToken = "test-provider-token"
        provider = Kakao
    }

    @OptIn(SupabaseInternal::class)
    private fun client(engine: MockEngine): SupabaseClient =
        createSupabaseClient("https://test.supabase.co", "test-key") {
            httpEngine = engine
            install(Auth) {
                minimalConfig()
                autoSetupPlatform = false
            }
            install(Postgrest)
        }

    private companion object {
        const val TOKEN_PATH = "/auth/v1/token"
        const val ACCEPT_PATH = "/rest/v1/rpc/accept_signup_terms"
        const val CHALLENGE_ID = "44bef24e-d47a-4a25-8e3a-c98cd8bfdde7"
        val JSON_HEADERS = headersOf("Content-Type", "application/json")
        val CHALLENGE_RESPONSE =
            buildJsonObject {
                put(
                    "msg",
                    "RAMAP_SIGNUP_TERMS_REQUIRED " +
                        buildJsonObject {
                            put("challenge_id", CHALLENGE_ID)
                            put("version", "v1")
                            put("url", "https://example.test/terms/v1")
                        },
                )
            }.toString()
        const val SESSION_BODY =
            """{"access_token":"test-access-token","refresh_token":"test-refresh-token","token_type":"bearer","expires_in":3600}"""
    }
}
