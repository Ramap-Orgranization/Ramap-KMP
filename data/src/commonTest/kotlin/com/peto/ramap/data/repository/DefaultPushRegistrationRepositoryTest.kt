package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.notification.PushRegistrationDataSource
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultPushRegistrationRepositoryTest {
    @Test
    fun `변경된 계정의 식별자를 이전 소유자로 등록하지 않는다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "account-b")
            val registrations = mutableListOf<String>()
            val remote =
                object : PushRegistrationDataSource {
                    override suspend fun register(
                        identifier: String,
                        platform: String,
                        targetType: String,
                        expectedAccountId: String,
                        supportedProtocolVersion: Int,
                    ) {
                        registrations += expectedAccountId
                    }
                }
            val repository = DefaultPushRegistrationRepository(remote, login)
            assertFailsWith<CancellationException> { repository.register("device", "ios", "token", "account-a", 1) }
            assertEquals(emptyList(), registrations)
            repository.register("device", "ios", "token", "account-b", 1)
            assertEquals(listOf("account-b"), registrations)
        }
}
