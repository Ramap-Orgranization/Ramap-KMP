package com.peto.ramap.network.auth

import kotlinx.coroutines.CompletableDeferred

internal class FakeSessionStorage(
    var stored: String? = null,
) : SessionStorage {
    var legacyStored: String? = null
    var legacyDeleteFailure: Exception? = null
    var legacyReads = 0
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var verificationFailure: Exception? = null
    var writeGate: CompletableDeferred<Unit>? = null
    val writeStarted = CompletableDeferred<Unit>()
    var writes = 0

    override suspend fun readLegacy(): String? {
        legacyReads += 1
        return legacyStored
    }

    override suspend fun deleteLegacy() {
        legacyDeleteFailure?.let { throw it }
        legacyStored = null
    }

    override suspend fun read(): String? {
        readFailure?.let { throw it }
        return stored
    }

    override suspend fun write(value: String) {
        writeStarted.complete(Unit)
        writeGate?.await()
        writeFailure?.let { throw it }
        stored = value
        writes += 1
        readFailure = verificationFailure
    }
}
