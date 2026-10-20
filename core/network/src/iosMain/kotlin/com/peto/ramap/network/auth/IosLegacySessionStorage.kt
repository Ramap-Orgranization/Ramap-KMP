package com.peto.ramap.network.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSUserDefaults

internal class IosLegacySessionStorage(
    private val defaults: NSUserDefaults,
    private val keys: SessionStorageKeys,
) : LegacySessionStorage {
    override suspend fun read(): String? =
        withContext(Dispatchers.IO) {
            defaults.stringForKey(keys.session)
                ?: defaults.stringForKey(SessionStorageKeys.LEGACY_SESSION)
        }

    override suspend fun delete() {
        withContext(Dispatchers.IO) {
            defaults.removeObjectForKey(keys.session)
            defaults.removeObjectForKey(SessionStorageKeys.LEGACY_SESSION)
        }
    }
}
