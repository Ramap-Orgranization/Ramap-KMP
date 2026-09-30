package com.peto.ramap.debug.admin.ui.login

import com.peto.ramap.coroutinesTest
import com.peto.ramap.debug.admin.data.datasource.AdminAccessDataSource
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginError
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginIntent
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AdminLoginViewModelTest {
    @Test
    fun `administrator must press login before opening registration`() =
        coroutinesTest {
            val loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val accessDataSource = FakeAdminAccessDataSource()
            val viewModel = AdminLoginViewModel(loginRepository, accessDataSource)
            runCurrent()

            assertFalse(viewModel.uiState.value.isAuthorized)
            assertEquals(0, accessDataSource.checkCount)

            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value.isAuthorized)
            assertEquals(1, loginRepository.signInWithKakaoCallCount)
            assertEquals(1, accessDataSource.checkCount)

            loginRepository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()
            assertFalse(viewModel.uiState.value.isAuthorized)
        }

    @Test
    fun `account without administrator access stays on login page`() =
        coroutinesTest {
            val viewModel =
                AdminLoginViewModel(
                    FakeLoginRepository(),
                    FakeAdminAccessDataSource(allowed = false),
                )

            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertFalse(viewModel.uiState.value.isAuthorized)
            assertEquals(AdminLoginError.AccessDenied, viewModel.uiState.value.error)
        }

    @Test
    fun `access check failure can be retried with the same button`() =
        coroutinesTest {
            val accessDataSource = FakeAdminAccessDataSource(failure = IllegalStateException("network"))
            val viewModel = AdminLoginViewModel(FakeLoginRepository(), accessDataSource)
            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertEquals(AdminLoginError.AccessUnavailable, viewModel.uiState.value.error)
            accessDataSource.failure = null
            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value.isAuthorized)
            assertEquals(2, accessDataSource.checkCount)
        }

    private class FakeAdminAccessDataSource(
        private val allowed: Boolean = true,
        var failure: Throwable? = null,
    ) : AdminAccessDataSource {
        var checkCount = 0
            private set

        override suspend fun hasAccess(): Boolean {
            checkCount += 1
            failure?.let { throw it }
            return allowed
        }
    }
}
