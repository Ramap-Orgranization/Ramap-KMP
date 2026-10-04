package com.peto.ramap.domain.usecase

import app.cash.turbine.test
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginSessionUseCaseTest {
    @Test
    fun `세션 상태의 변경을 올바르게 방출한다`() =
        runTest {
            val repository = FakeLoginRepository(LoginSessionState.NOT_AUTHENTICATED)
            val useCase = LoginSessionUseCase(repository)

            useCase().test {
                assertEquals(LoginSessionState.NOT_AUTHENTICATED, awaitItem())

                repository.updateSessionState(LoginSessionState.AUTHENTICATED)
                assertEquals(LoginSessionState.AUTHENTICATED, awaitItem())
            }
        }

    @Test
    fun `세션 유무 및 현재 유저 이메일을 바르게 조회한다`() =
        runTest {
            val repository =
                FakeLoginRepository(
                    initialSessionState = LoginSessionState.AUTHENTICATED,
                    userEmail = "user@ramap.com",
                )
            val useCase = LoginSessionUseCase(repository)

            assertTrue(useCase.hasSession())
            assertEquals("user@ramap.com", useCase.currentUserEmail())

            repository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            assertFalse(useCase.hasSession())
        }
}
