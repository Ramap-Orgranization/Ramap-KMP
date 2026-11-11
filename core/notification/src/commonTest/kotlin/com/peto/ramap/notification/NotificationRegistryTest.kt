package com.peto.ramap.notification

import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.PushRegistrationRepository
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRegistryTest {
    @Test
    fun `인증된 계정이 생긴 뒤 기기를 등록한다`() =
        runTest {
            val loginRepository = FakeLoginRepository()
            val registrations = mutableListOf<String>()
            val repository = recordingRepository(registrations)
            val registry = NotificationRegistry(loginRepository, repository, backgroundScope)

            registry.track("identifier", "android", "fid")
            runCurrent()
            assertEquals(emptyList(), registrations)

            loginRepository.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()

            assertEquals(listOf("identifier"), registrations)
        }

    @Test
    fun `빈 식별자는 무시하고 이전 등록을 교체한다`() =
        runTest {
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val registrations = mutableListOf<String>()
            val repository = recordingRepository(registrations)
            val registry = NotificationRegistry(loginRepository, repository, backgroundScope)

            registry.track("first", "android", "fid")
            registry.track(" ", "android", "fid")
            registry.track("second", "android", "fid")
            runCurrent()

            assertEquals(listOf("second"), registrations)
        }

    @Test
    fun `등록 실패는 앱 동작을 중단하지 않는다`() =
        runTest {
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val repository =
                object : PushRegistrationRepository {
                    override suspend fun register(
                        identifier: String,
                        platform: String,
                        targetType: String,
                        expectedAccountId: String,
                        supportedProtocolVersion: Int,
                    ) {
                        error("registration failed")
                    }
                }
            val registry = NotificationRegistry(loginRepository, repository, backgroundScope)

            registry.track("identifier", "android", "fid")
            runCurrent()
        }

    @Test
    fun `로그인 상태가 같아도 계정이 바뀌면 새 소유자와 지원 버전으로 등록한다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "account-a")
            val owners = mutableListOf<Pair<String, Int>>()
            val repository =
                object : PushRegistrationRepository {
                    override suspend fun register(
                        identifier: String,
                        platform: String,
                        targetType: String,
                        expectedAccountId: String,
                        supportedProtocolVersion: Int,
                    ) {
                        owners += expectedAccountId to supportedProtocolVersion
                    }
                }
            val registry = NotificationRegistry(login, repository, backgroundScope)
            registry.track("device", "ios", "token")
            runCurrent()
            login.updateAccountId("account-b")
            runCurrent()
            login.updateAccountId(null)
            runCurrent()
            assertEquals(listOf("account-a" to 3, "account-b" to 3), owners)
        }

    private fun recordingRepository(registrations: MutableList<String>): PushRegistrationRepository =
        object : PushRegistrationRepository {
            override suspend fun register(
                identifier: String,
                platform: String,
                targetType: String,
                expectedAccountId: String,
                supportedProtocolVersion: Int,
            ) {
                registrations += identifier
            }
        }
}
