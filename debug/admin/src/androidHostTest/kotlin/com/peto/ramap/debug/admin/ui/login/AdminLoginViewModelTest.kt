package com.peto.ramap.debug.admin.ui.login

import com.peto.ramap.coroutinesTest
import com.peto.ramap.debug.admin.data.datasource.AdminAccessDataSource
import com.peto.ramap.debug.admin.data.datasource.AdminAuthDataSource
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginError
import com.peto.ramap.debug.admin.ui.login.contract.AdminLoginIntent
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
            val authDataSource = FakeAdminAuthDataSource()
            val accessDataSource = FakeAdminAccessDataSource()
            val viewModel = AdminLoginViewModel(authDataSource, accessDataSource)
            runCurrent()

            assertFalse(viewModel.uiState.value.isAuthorized)
            assertEquals(0, accessDataSource.checkCount)

            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value.isAuthorized)
            assertEquals(1, authDataSource.signInCount)
            assertEquals(1, accessDataSource.checkCount)
        }

    @Test
    fun `account without administrator access stays on login page`() =
        coroutinesTest {
            val viewModel =
                AdminLoginViewModel(
                    FakeAdminAuthDataSource(),
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
            val viewModel = AdminLoginViewModel(FakeAdminAuthDataSource(), accessDataSource)
            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertEquals(AdminLoginError.AccessUnavailable, viewModel.uiState.value.error)
            accessDataSource.failure = null
            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertTrue(viewModel.uiState.value.isAuthorized)
            assertEquals(2, accessDataSource.checkCount)
        }

    @Test
    fun `failed credentials do not check administrator access`() =
        coroutinesTest {
            val accessDataSource = FakeAdminAccessDataSource()
            val viewModel =
                AdminLoginViewModel(
                    FakeAdminAuthDataSource(
                        failure = IllegalArgumentException("credentials"),
                    ),
                    accessDataSource,
                )

            viewModel.dispatch(AdminLoginIntent.OnAdminLoginClicked)
            runCurrent()

            assertFalse(viewModel.uiState.value.isAuthorized)
            assertEquals(AdminLoginError.LoginFailed, viewModel.uiState.value.error)
            assertEquals(0, accessDataSource.checkCount)
        }

    private class FakeAdminAuthDataSource(
        private val failure: Throwable? = null,
    ) : AdminAuthDataSource {
        var signInCount = 0
            private set

        override suspend fun signIn() {
            signInCount += 1
            failure?.let { throw it }
        }
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
