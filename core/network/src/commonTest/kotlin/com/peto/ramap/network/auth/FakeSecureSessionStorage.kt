package com.peto.ramap.network.auth

import kotlinx.coroutines.CompletableDeferred

internal class FakeSecureSessionStorage(
    var stored: String? = null,
) : SecureSessionStorage {
    var readFailure: Exception? = null
    var writeFailure: Exception? = null
    var verificationFailure: Exception? = null
    var writeGate: CompletableDeferred<Unit>? = null
    val writeStarted = CompletableDeferred<Unit>()
    var writes = 0

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
