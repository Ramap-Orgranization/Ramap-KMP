package com.peto.ramap.network.auth

internal interface LegacySessionStorage {
    suspend fun read(): String?

    suspend fun delete()
}
