package com.peto.ramap.network.auth

import android.content.SharedPreferences
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

internal class AndroidSessionStorage(
    private val file: File,
    private val cipher: AndroidSessionCipher,
    private val legacyPreferences: SharedPreferences,
    private val keys: SessionStorageKeys,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) : SessionStorage {
    private val dataStore =
        PreferenceDataStoreFactory.create(
            // 손상된 저장소를 미이관 상태로 되돌리면 예전 로그인 정보가 부활할 수 있습니다.
            corruptionHandler = ReplaceFileCorruptionHandler { preferencesOf(SESSION to cipher.encrypt(byteArrayOf())) },
            scope = scope,
            produceFile = { file },
        )

    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            val encrypted = dataStore.data.first()[SESSION]
            if (encrypted == null) {
                // 신규 설치의 파일 부재와 기존 파일에서 세션 값이 유실된 상태를 구분합니다.
                if (file.exists()) throw InvalidStoredSessionException()
                return@withContext null
            }
            cipher.decrypt(encrypted).decodeToString()
        }

    override suspend fun write(value: String) {
        withContext(Dispatchers.IO) {
            val encrypted = cipher.encrypt(value.encodeToByteArray())
            dataStore.edit { it[SESSION] = encrypted }
        }
    }

    override suspend fun readLegacy(): String? =
        withContext(Dispatchers.IO) {
            legacyPreferences.getString(keys.session, null)
                ?: legacyPreferences.getString(SessionStorageKeys.LEGACY_SESSION, null)
        }

    override suspend fun deleteLegacy() {
        withContext(Dispatchers.IO) {
            val removed =
                legacyPreferences
                    .edit()
                    .remove(keys.session)
                    .remove(SessionStorageKeys.LEGACY_SESSION)
                    .commit()
            if (!removed) throw IOException(ERROR_DELETE)
        }
    }

    private companion object {
        val SESSION = byteArrayPreferencesKey("encrypted_session")
        const val ERROR_DELETE = "Legacy session removal did not complete"
    }
}
