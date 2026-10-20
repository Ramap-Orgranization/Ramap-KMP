package com.peto.ramap.network.auth

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(SupabaseInternal::class)
class SupabaseSessionPersistenceTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `SDK 초기화가 기존 세션을 이관하고 갱신 및 삭제를 보안 저장소에 반영한다`() =
        runTest {
            val session =
                UserSession(
                    accessToken = "test-access",
                    refreshToken = "test-refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                )
            val storage = FakeSessionStorage()
            storage.legacyStored = json.encodeToString(session)
            val manager = MigratingSessionManager(storage)
            val client = createTestClient(manager)
            try {
                client.auth.awaitInitialization()
                assertEquals(session, client.auth.currentSessionOrNull())
                assertNull(storage.legacyStored)

                val refreshed = session.copy(accessToken = "new-access", refreshToken = "new-refresh")
                client.auth.importSession(refreshed, autoRefresh = false)
                assertEquals(refreshed, MigratingSessionManager(storage).loadSession())

                client.auth.clearSession()
                assertNull(client.auth.currentSessionOrNull())
                assertNull(MigratingSessionManager(storage).loadSessionOrNull())
            } finally {
                client.close()
            }
        }

    private fun createTestClient(manager: SessionManager) =
        createSupabaseClient("https://example.com", "test-key") {
            install(Auth) {
                minimalConfig()
                autoSetupPlatform = false
                autoLoadFromStorage = true
                autoSaveToStorage = true
                sessionManager = manager
            }
        }
}
