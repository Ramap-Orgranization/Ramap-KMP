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
    fun waitsForAuthenticatedSessionBeforeRegistering() =
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
    fun ignoresBlankIdentifierAndCancelsPreviousRegistration() =
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
    fun swallowsRegistrationFailure() =
        runTest {
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val repository =
                object : PushRegistrationRepository {
                    override suspend fun register(
                        identifier: String,
                        platform: String,
                        targetType: String,
                    ) {
                        error("registration failed")
                    }
                }
            val registry = NotificationRegistry(loginRepository, repository, backgroundScope)

            registry.track("identifier", "android", "fid")
            runCurrent()
        }

    private fun recordingRepository(registrations: MutableList<String>): PushRegistrationRepository =
        object : PushRegistrationRepository {
            override suspend fun register(
                identifier: String,
                platform: String,
                targetType: String,
            ) {
                registrations += identifier
            }
        }
}
