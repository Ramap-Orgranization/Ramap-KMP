package com.peto.ramap.ui.main.my

import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun `로그인 세션 변경을 설정 상태에 반영한다`() =
        coroutinesTest {
            val repository = FakeLoginRepository()
            val viewModel = SettingsViewModel(repository)
            runCurrent()

            repository.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()

            assertTrue(viewModel.uiState.value.isLoggedIn)

            repository.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoggedIn)
        }
}
