package com.peto.ramap.network.auth

import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MigratingSessionManagerTest {
    private val json = Json { encodeDefaults = true }
    private val session =
        UserSession(
            accessToken = "test-access",
            refreshToken = "test-refresh",
            expiresIn = 3600,
            tokenType = "bearer",
            user = UserInfo(id = "test-user", aud = "authenticated"),
        )
    private val encoded = json.encodeToString(session)

    @Test
    fun `이관 후 토큰과 만료 정보 및 사용자 정보를 그대로 복원하고 기존 값을 삭제한다`() =
        runTest {
            val storage = FakeSessionStorage()
            storage.legacyStored = encoded

            assertEquals(session, MigratingSessionManager(storage).loadSession())
            assertEquals(encoded, storage.stored)
            assertNull(storage.legacyStored)
            assertEquals(session, MigratingSessionManager(storage).loadSession())
            assertEquals(1, storage.legacyReads)
        }

    @Test
    fun `이관 저장이 실패하면 기존 세션을 보존한다`() =
        runTest {
            val storage = FakeSessionStorage().apply { writeFailure = IllegalStateException() }
            storage.legacyStored = encoded

            assertFailsWith<IllegalStateException> { MigratingSessionManager(storage).loadSession() }
            assertEquals(encoded, storage.legacyStored)
            assertNull(storage.stored)
        }

    @Test
    fun `이관 직후 검증이 실패해도 기존 세션을 삭제하지 않는다`() =
        runTest {
            val storage = FakeSessionStorage().apply { verificationFailure = InvalidStoredSessionException() }
            storage.legacyStored = encoded

            assertFailsWith<InvalidStoredSessionException> { MigratingSessionManager(storage).loadSession() }
            assertEquals(encoded, storage.legacyStored)
            assertEquals(1, storage.writes)
        }

    @Test
    fun `저장소 접근 오류를 세션 손상으로 취급하거나 기존 저장소로 우회하지 않는다`() =
        runTest {
            val storage = FakeSessionStorage(encoded).apply { readFailure = IllegalStateException() }
            storage.legacyStored = encoded

            assertFailsWith<IllegalStateException> { MigratingSessionManager(storage).loadSessionOrNull() }
            assertEquals(encoded, storage.stored)
            assertEquals(encoded, storage.legacyStored)
            assertEquals(0, storage.legacyReads)
        }

    @Test
    fun `보안 저장소가 있으면 남아 있는 이전 세션보다 우선한다`() =
        runTest {
            val updated = session.copy(accessToken = "new-access", refreshToken = "new-refresh")
            val storage = FakeSessionStorage(json.encodeToString(updated))
            storage.legacyStored = encoded

            assertEquals(updated, MigratingSessionManager(storage).loadSession())
            assertNull(storage.legacyStored)
            assertEquals(0, storage.legacyReads)
        }

    @Test
    fun `구 저장소 정리 실패는 로그인을 막지 않고 다음 복원에서 정리한다`() =
        runTest {
            val storage = FakeSessionStorage()
            storage.legacyStored = encoded
            storage.legacyDeleteFailure = IllegalStateException()

            assertEquals(session, MigratingSessionManager(storage).loadSession())
            assertEquals(encoded, storage.legacyStored)
            storage.legacyDeleteFailure = null
            assertEquals(session, MigratingSessionManager(storage).loadSession())
            assertNull(storage.legacyStored)
        }

    @Test
    fun `로그아웃 중 구 저장소 삭제가 실패해도 재시작 후 세션이 부활하지 않는다`() =
        runTest {
            val storage = FakeSessionStorage(encoded)
            storage.legacyStored = encoded
            storage.legacyDeleteFailure = IllegalStateException()

            MigratingSessionManager(storage).deleteSession()

            assertEquals("", storage.stored)
            assertNull(MigratingSessionManager(storage).loadSessionOrNull())
            assertEquals(0, storage.legacyReads)
        }

    @Test
    fun `로그아웃 저장 실패는 호출자에게 전달하고 기존 세션을 보존한다`() =
        runTest {
            val storage = FakeSessionStorage(encoded).apply { writeFailure = IllegalStateException() }
            storage.legacyStored = encoded

            assertFailsWith<IllegalStateException> { MigratingSessionManager(storage).deleteSession() }
            assertEquals(encoded, storage.stored)
            assertEquals(encoded, storage.legacyStored)
        }

    @Test
    fun `암호문이나 키가 손상되면 이전 토큰을 복원하지 않고 삭제 상태를 저장한다`() =
        runTest {
            val storage = FakeSessionStorage(encoded).apply { readFailure = InvalidStoredSessionException() }
            storage.legacyStored = encoded

            assertNull(MigratingSessionManager(storage).loadSessionOrNull())
            assertEquals("", storage.stored)
            assertNull(storage.legacyStored)
            assertEquals(0, storage.legacyReads)
        }

    @Test
    fun `손상된 세션 JSON은 민감한 원문을 예외로 노출하지 않고 정리한다`() =
        runTest {
            val storage = FakeSessionStorage("invalid-json-test-token")
            storage.legacyStored = encoded

            assertNull(MigratingSessionManager(storage).loadSessionOrNull())
            assertEquals("", storage.stored)
            assertNull(storage.legacyStored)
        }

    @Test
    fun `갱신한 토큰 쌍은 새 매니저에서도 함께 복원된다`() =
        runTest {
            val storage = FakeSessionStorage(encoded)
            val updated = session.copy(accessToken = "updated-access", refreshToken = "updated-refresh")

            MigratingSessionManager(storage).saveSession(updated)

            assertEquals(updated, MigratingSessionManager(storage).loadSession())
        }

    @Test
    fun `이관 중 로그아웃이 들어오면 이관 완료 뒤 삭제하여 세션을 남기지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val storage = FakeSessionStorage().apply { writeGate = gate }
            storage.legacyStored = encoded
            val manager = MigratingSessionManager(storage)
            val migration = launch { manager.loadSession() }
            storage.writeStarted.await()
            val logout = launch { manager.deleteSession() }
            gate.complete(Unit)
            migration.join()
            logout.join()

            assertNull(MigratingSessionManager(storage).loadSessionOrNull())
            assertNull(storage.legacyStored)
        }

    @Test
    fun `저장 중 호출자가 취소되어도 저장과 구 저장소 정리를 끝낸다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val storage = FakeSessionStorage().apply { writeGate = gate }
            storage.legacyStored = encoded
            val manager = MigratingSessionManager(storage)
            val migration = launch { manager.loadSession() }
            storage.writeStarted.await()
            migration.cancel()
            gate.complete(Unit)
            migration.join()

            assertEquals(session, MigratingSessionManager(storage).loadSession())
            assertNull(storage.legacyStored)
        }

    @Test
    fun `신규 설치에는 복원할 세션이 없다`() =
        runTest {
            assertNull(MigratingSessionManager(FakeSessionStorage()).loadSessionOrNull())
        }
}
