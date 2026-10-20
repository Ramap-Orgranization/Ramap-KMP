package com.peto.ramap.network.auth

import co.touchlab.kermit.Logger
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class MigratingSessionManager(
    private val secureStorage: SecureSessionStorage,
    private val legacyStorage: LegacySessionStorage,
) : SessionManager {
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    override suspend fun loadSession(): UserSession = loadSessionOrNull() ?: error(ERROR_NO_SESSION)

    override suspend fun loadSessionOrNull(): UserSession? =
        mutex.withLock {
            withContext(NonCancellable) {
                restoreSession()
            }
        }

    override suspend fun saveSession(session: UserSession) {
        mutex.withLock {
            withContext(NonCancellable) {
                persistSession(json.encodeToString(session))
            }
        }
    }

    override suspend fun deleteSession() {
        mutex.withLock {
            withContext(NonCancellable) {
                // 삭제 기록을 먼저 저장해 남아 있는 구 저장소에서 세션이 부활하지 않게 합니다.
                persistSession(CLEARED_SESSION)
            }
        }
    }

    private suspend fun restoreSession(): UserSession? {
        val secured =
            try {
                secureStorage.read()
            } catch (_: InvalidStoredSessionException) {
                persistSession(CLEARED_SESSION)
                return null
            }
        if (secured != null) {
            val session = decodeOrClearSession(secured)
            deleteLegacySession()
            return session
        }

        val legacy = legacyStorage.read() ?: return null
        val session = decodeOrClearSession(legacy) ?: return null
        secureStorage.write(legacy)
        check(secureStorage.read() == legacy) { ERROR_MIGRATION_VERIFICATION }
        deleteLegacySession()
        return session
    }

    private suspend fun decodeOrClearSession(value: String): UserSession? {
        if (value == CLEARED_SESSION) return null
        return try {
            json.decodeFromString<UserSession>(value)
        } catch (_: SerializationException) {
            persistSession(CLEARED_SESSION)
            null
        } catch (_: IllegalArgumentException) {
            persistSession(CLEARED_SESSION)
            null
        }
    }

    private suspend fun persistSession(value: String) {
        secureStorage.write(value)
        deleteLegacySession()
    }

    private suspend fun deleteLegacySession() {
        try {
            legacyStorage.delete()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // 새 저장소가 이미 원본이므로 로그인은 유지하고 다음 접근 때 정리를 재시도합니다.
            Logger.w(tag = TAG) { ERROR_LEGACY_CLEANUP }
        }
    }

    private companion object {
        const val TAG = "SessionStorage"
        const val CLEARED_SESSION = ""
        const val ERROR_NO_SESSION = "No session stored"
        const val ERROR_MIGRATION_VERIFICATION = "Session migration verification failed"
        const val ERROR_LEGACY_CLEANUP = "Legacy session cleanup will be retried"
    }
}
