package com.peto.ramap.ui.account

import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.ui.account.contract.AccountIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @Test
    fun `로그인 세션이면 계정 이메일을 표시한다`() =
        coroutinesTest {
            val viewModel =
                AccountViewModel(
                    FakeLoginRepository(
                        initialSessionState = LoginSessionState.AUTHENTICATED,
                        userEmail = "test@ramap.com",
                    ),
                    LoginAnalytics(FakeAnalyticsTracker()),
                )

            runCurrent()

            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertEquals("test@ramap.com", viewModel.uiState.value.accountLabel)
        }

    @Test
    fun `계정 삭제를 확인하면 저장소에 요청한다`() =
        coroutinesTest {
            val repository =
                FakeLoginRepository(initialSessionState = LoginSessionState.AUTHENTICATED)
            val viewModel = AccountViewModel(repository, LoginAnalytics(FakeAnalyticsTracker()))
            runCurrent()

            viewModel.dispatch(AccountIntent.OnAccountDeleteConfirm)
            runCurrent()

            assertEquals(1, repository.deleteAccountCallCount)
        }
}
