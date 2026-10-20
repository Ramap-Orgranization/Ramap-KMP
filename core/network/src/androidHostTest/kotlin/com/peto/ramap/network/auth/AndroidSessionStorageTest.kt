package com.peto.ramap.network.auth

import android.content.Context
import android.content.SharedPreferences
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidSessionStorageTest {
    private val keys = SessionStorageKeys("https://test.supabase.co")
    private val session = UserSession(accessToken = "test-access", refreshToken = "test-refresh", expiresIn = 3600, tokenType = "bearer")
    private val json = Json { encodeDefaults = true }
    private val encoded = json.encodeToString(session)

    @Test
    fun `DataStore를 다시 열어 세션과 로그아웃 상태를 복원하고 파일에는 평문을 남기지 않는다`() =
        runTest {
            val file = sessionFile()
            val provider = FakeSessionEncryptionKeyProvider()
            val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            try {
                val store = storage(file, provider, scope)
                assertNull(store.read())
                store.write(encoded)
                assertFalse(file.readText().contains("test-access"))
                assertFalse(file.readText().contains("test-refresh"))
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
            }

            val restoredScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            try {
                val restored = storage(file, provider, restoredScope)
                assertEquals(encoded, restored.read())
                restored.write("")
            } finally {
                restoredScope.coroutineContext[Job]!!.cancelAndJoin()
            }
            assertEquals("", storage(file, provider, backgroundScope).read())
        }

    @Test
    fun `SDK의 현재 키와 예전 키를 모두 읽고 세션만 삭제한다`() =
        runTest {
            val preferences = legacyPreferences()
            val store = storage(sessionFile(), FakeSessionEncryptionKeyProvider(), backgroundScope, preferences)
            preferences
                .edit()
                .putString(keys.session, "current-session")
                .putString(SettingsSessionManager.SETTINGS_KEY, "old-session")
                .putString("unrelated-setting", "preserved")
                .commit()

            assertEquals("current-session", store.readLegacy())
            preferences.edit().remove(keys.session).commit()
            assertEquals("old-session", store.readLegacy())
            store.deleteLegacy()
            assertNull(store.readLegacy())
            assertEquals("preserved", preferences.getString("unrelated-setting", null))
        }

    @Test
    fun `기존 세션을 DataStore에 이관한 뒤 재시작해도 로그인을 유지한다`() =
        runTest {
            val preferences = legacyPreferences()
            preferences
                .edit()
                .putString(keys.session, encoded)
                .putString("unrelated-setting", "preserved")
                .commit()
            val file = sessionFile()
            val provider = FakeSessionEncryptionKeyProvider()
            val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
            try {
                val manager = MigratingSessionManager(storage(file, provider, scope, preferences))
                assertEquals(session, manager.loadSession())
                assertNull(preferences.getString(keys.session, null))
                assertEquals("preserved", preferences.getString("unrelated-setting", null))
            } finally {
                scope.coroutineContext[Job]!!.cancelAndJoin()
            }

            assertEquals(session, MigratingSessionManager(storage(file, provider, backgroundScope, preferences)).loadSession())
        }

    @Test
    fun `DataStore 파일이 손상되어도 남아 있는 예전 로그인으로 돌아가지 않는다`() =
        runTest {
            val preferences = legacyPreferences()
            preferences.edit().putString(keys.session, encoded).commit()
            val file = sessionFile().apply { writeBytes(byteArrayOf(-1)) }
            val store = storage(file, FakeSessionEncryptionKeyProvider(), backgroundScope, preferences)

            assertNull(MigratingSessionManager(store).loadSessionOrNull())
            assertEquals("", store.read())
            assertNull(preferences.getString(keys.session, null))
            MigratingSessionManager(store).saveSession(session)
            assertEquals(session, MigratingSessionManager(store).loadSession())
        }

    @Test
    fun `기존 DataStore 파일이 비어 있어도 미이관 상태로 오인하지 않는다`() =
        runTest {
            val preferences = legacyPreferences()
            preferences.edit().putString(keys.session, encoded).commit()
            val file = sessionFile().apply { writeBytes(byteArrayOf()) }
            val store = storage(file, FakeSessionEncryptionKeyProvider(), backgroundScope, preferences)

            assertNull(MigratingSessionManager(store).loadSessionOrNull())
            assertEquals("", store.read())
            assertNull(preferences.getString(keys.session, null))
        }

    @Test
    fun `DataStore 저장 경로를 만들 수 없으면 기존 세션을 삭제하지 않는다`() =
        runTest {
            val preferences = legacyPreferences()
            preferences.edit().putString(keys.session, encoded).commit()
            val parentFile = sessionFile().apply { writeText("not-a-directory") }
            val file = File(parentFile, "session.preferences_pb")
            val store = storage(file, FakeSessionEncryptionKeyProvider(), backgroundScope, preferences)

            assertFailsWith<IOException> { MigratingSessionManager(store).loadSession() }
            assertEquals(encoded, preferences.getString(keys.session, null))
        }

    private fun sessionFile(): File = File.createTempFile("session-", ".preferences_pb", RuntimeEnvironment.getApplication().noBackupFilesDir).apply { delete() }

    private fun legacyPreferences(): SharedPreferences = RuntimeEnvironment.getApplication().getSharedPreferences("legacy-test", Context.MODE_PRIVATE)

    private fun storage(
        file: File,
        provider: FakeSessionEncryptionKeyProvider,
        scope: CoroutineScope,
        preferences: SharedPreferences = legacyPreferences(),
    ) = AndroidSessionStorage(file, AndroidSessionCipher(provider), preferences, keys, scope)
}
