package com.peto.ramap.data.datasource.notification

internal interface PushRegistrationDataSource {
    suspend fun register(
        identifier: String,
        platform: String,
        targetType: String,
        expectedAccountId: String,
        supportedProtocolVersion: Int,
    )
}
