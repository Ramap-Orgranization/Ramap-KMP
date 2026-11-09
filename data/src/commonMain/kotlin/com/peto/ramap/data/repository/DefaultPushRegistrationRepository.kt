package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.notification.PushRegistrationDataSource
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PushRegistrationRepository
import kotlinx.coroutines.CancellationException

internal class DefaultPushRegistrationRepository(
    private val dataSource: PushRegistrationDataSource,
    private val loginRepository: LoginRepository,
) : PushRegistrationRepository {
    override suspend fun register(
        identifier: String,
        platform: String,
        targetType: String,
        expectedAccountId: String,
        supportedProtocolVersion: Int,
    ) {
        if (identifier.isBlank()) return
        if (loginRepository.currentAccountId() != expectedAccountId) throw CancellationException(ACCOUNT_CHANGED)
        dataSource.register(identifier, platform, targetType, expectedAccountId, supportedProtocolVersion)
        if (loginRepository.currentAccountId() != expectedAccountId) throw CancellationException(ACCOUNT_CHANGED)
    }

    private companion object {
        const val ACCOUNT_CHANGED = "Push registration account changed"
    }
}
