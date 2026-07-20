package com.peto.ramap.ui.notification

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.ui.common.RamapUiState
import com.peto.ramap.ui.notification.contract.NotificationSettingsIntent
import kotlinx.coroutines.CompletableDeferred
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

            assertEquals(RamapUiState.Success(Unit), viewModel.uiState.value.ramapUiState)
            assertTrue(viewModel.uiState.value.notificationEnabled)
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

            assertEquals(RamapUiState.Error, viewModel.uiState.value.ramapUiState)
        }

    @Test
    fun `알림 토글을 끄면 저장소 상태를 갱신한다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository(enabled = true)
            val viewModel = NotificationSettingsViewModel(repository)
            runCurrent()

            viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(false))
            runCurrent()

            assertFalse(viewModel.uiState.value.notificationEnabled)
            assertEquals(listOf(false), repository.enabledUpdates)
        }

    @Test
    fun `빠른 알림 토글은 이전 요청을 취소하고 최신 optimistic 값을 유지한다`() =
        coroutinesTest {
            val firstResult = CompletableDeferred<RamapResult<Unit>>()
            val updates = mutableListOf<Boolean>()
            val repository =
                object : NotificationSettingsRepository by FakeNotificationSettingsRepository(enabled = false) {
                    override suspend fun updateEventNotificationsEnabled(enabled: Boolean): RamapResult<Unit> {
                        updates += enabled
                        return if (enabled) firstResult.await() else RamapResult.Success(Unit)
                    }
                }
            val viewModel = NotificationSettingsViewModel(repository)
            runCurrent()

            viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(true))
            runCurrent()
            assertTrue(viewModel.uiState.value.notificationEnabled)

            viewModel.dispatch(NotificationSettingsIntent.OnEventNotificationsEnabledChanged(false))
            runCurrent()

            assertFalse(viewModel.uiState.value.notificationEnabled)
            assertEquals(listOf(true, false), updates)

            firstResult.complete(RamapResult.Error(RamapError.Unknown(IllegalStateException("late"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.notificationEnabled)
        }
}
