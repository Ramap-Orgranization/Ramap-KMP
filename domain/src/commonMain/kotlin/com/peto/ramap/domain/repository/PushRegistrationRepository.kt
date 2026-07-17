package com.peto.ramap.domain.repository

interface PushRegistrationRepository {
    suspend fun register(
        identifier: String,
        platform: String,
        targetType: String,
    )
}
