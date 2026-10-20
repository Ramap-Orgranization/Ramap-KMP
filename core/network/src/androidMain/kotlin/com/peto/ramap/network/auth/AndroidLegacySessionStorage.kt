package com.peto.ramap.network.auth

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal class AndroidLegacySessionStorage(
    private val preferences: SharedPreferences,
    private val keys: SessionStorageKeys,
) : LegacySessionStorage {
    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            preferences.getString(keys.session, null)
                ?: preferences.getString(SessionStorageKeys.LEGACY_SESSION, null)
        }

    override suspend fun delete() {
        withContext(Dispatchers.IO) {
            val removed =
                preferences
                    .edit()
                    .remove(keys.session)
                    .remove(SessionStorageKeys.LEGACY_SESSION)
                    .commit()
            if (!removed) throw IOException(ERROR_DELETE)
        }
    }

    private companion object {
        const val ERROR_DELETE = "Legacy session removal did not complete"
    }
}
