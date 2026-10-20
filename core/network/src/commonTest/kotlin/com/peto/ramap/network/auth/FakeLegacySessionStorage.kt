package com.peto.ramap.network.auth

internal class FakeLegacySessionStorage(
    var stored: String? = null,
) : LegacySessionStorage {
    var deleteFailure: Exception? = null
    var reads = 0

    override suspend fun read(): String? {
        reads += 1
        return stored
    }

    override suspend fun delete() {
        deleteFailure?.let { throw it }
        stored = null
    }
}
