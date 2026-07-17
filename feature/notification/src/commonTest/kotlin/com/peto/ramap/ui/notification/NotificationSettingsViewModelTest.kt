package com.peto.ramap.ui.notification

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @Test
    fun `서버 설정 상태만 알림 토글 화면에 표시한다`() =
        coroutinesTest {
            val viewModel =
                NotificationSettingsViewModel(
                    FakeNotificationSettingsRepository(enabled = true),
                )

            runCurrent()

            assertEquals(LoadState.Content(Unit), viewModel.uiState.value.loadState)
            assertTrue(viewModel.uiState.value.areEnabled)
        }

    @Test
    fun `서버 설정 조회에 실패하면 오류 상태를 표시한다`() =
        coroutinesTest {
            val repository =
                FakeNotificationSettingsRepository().apply {
                    fetchEnabledError = RamapError.Unknown(IllegalStateException("failure"))
                }
            val viewModel = NotificationSettingsViewModel(repository)

            runCurrent()

            assertEquals(LoadState.Error, viewModel.uiState.value.loadState)
        }

    @Test
    fun `알림 토글을 끄면 저장소 상태를 갱신한다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository(enabled = true)
            val viewModel = NotificationSettingsViewModel(repository)
            runCurrent()

            viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(false))
            runCurrent()

            assertFalse(viewModel.uiState.value.areEnabled)
            assertEquals(listOf(false), repository.enabledUpdates)
        }
}
