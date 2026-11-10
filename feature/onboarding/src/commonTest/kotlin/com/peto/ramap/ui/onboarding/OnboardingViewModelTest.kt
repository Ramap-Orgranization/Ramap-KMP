package com.peto.ramap.ui.onboarding

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeOnboardingStorage
import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @Test
    fun `권한 안내만 확인한 사용자는 온보딩을 마치지 않은 상태로 복원한다`() =
        coroutinesTest {
            val viewModel = createViewModel(FakeOnboardingStorage().apply { confirmed = true })
            runCurrent()
            assertEquals(OnboardingStep.Guide, viewModel.uiState.value.step)
        }

    @Test
    fun `매장 선택까지 완료하면 다음 실행에서 온보딩 전체를 건너뛴다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage().apply { confirmed = true }
            val viewModel = createViewModel(storage)
            runCurrent()
            viewModel.sideEffect.test {
                expectNoEvents()
                storage.completeOnboarding()
                repeat(2) { viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted) }
                runCurrent()
                assertEquals(OnboardingStep.Completed, viewModel.uiState.value.step)
                assertEquals(OnboardingSideEffect.Completed, awaitItem())
                expectNoEvents()
            }

            val reopened = createViewModel(storage)
            runCurrent()
            assertEquals(OnboardingStep.Completed, reopened.uiState.value.step)
            reopened.sideEffect.test {
                assertEquals(OnboardingSideEffect.Completed, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `확인 기록을 읽는 동안 메인 진입 여부를 확정하지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<Unit>()
            val storage = FakeOnboardingStorage().apply { pendingRead = pending }
            val viewModel = createViewModel(storage)
            runCurrent()

            assertEquals(OnboardingStep.Initializing, viewModel.uiState.value.step)
            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(OnboardingLoadKey.ReadConfirmation),
            )
            viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide)
            viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted)
            runCurrent()
            assertFalse(storage.confirmed)
            assertEquals(OnboardingStep.Initializing, viewModel.uiState.value.step)

            pending.complete(Unit)
            runCurrent()
            assertEquals(OnboardingStep.PermissionGuide, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
            viewModel.dispatch(OnboardingIntent.ShopSelectionCompleted)
            runCurrent()
            assertEquals(OnboardingStep.PermissionGuide, viewModel.uiState.value.step)
            viewModel.sideEffect.test { expectNoEvents() }
        }

    @Test
    fun `확인 전 다시 실행하면 안내를 표시하고 확인 후에는 건너뛴다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage()
            val firstLaunch = createViewModel(storage)
            runCurrent()
            assertEquals(OnboardingStep.PermissionGuide, firstLaunch.uiState.value.step)

            val beforeConfirmation = createViewModel(storage)
            runCurrent()
            assertEquals(OnboardingStep.PermissionGuide, beforeConfirmation.uiState.value.step)

            beforeConfirmation.dispatch(OnboardingIntent.ConfirmPermissionGuide)
            finishPermissionRequests(beforeConfirmation)
            runCurrent()
            assertEquals(OnboardingStep.Guide, beforeConfirmation.uiState.value.step)

            val afterConfirmation = createViewModel(storage)
            runCurrent()
            assertEquals(OnboardingStep.Guide, afterConfirmation.uiState.value.step)
        }

    @Test
    fun `저장 중 확인을 반복해도 저장이 끝나기 전에는 안내를 유지한다`() =
        coroutinesTest {
            val pending = CompletableDeferred<Unit>()
            val storage = FakeOnboardingStorage().apply { pendingSave = pending }
            val viewModel = createViewModel(storage)
            runCurrent()

            repeat(2) { viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide) }
            finishPermissionRequests(viewModel)
            runCurrent()
            assertEquals(OnboardingStep.PermissionGuide, viewModel.uiState.value.step)
            assertTrue(
                viewModel.uiState.value.loadState
                    .isLoading(OnboardingLoadKey.ConfirmPermissionGuide),
            )
            assertFalse(storage.confirmed)

            pending.complete(Unit)
            runCurrent()
            assertTrue(storage.confirmed)
            assertEquals(OnboardingStep.Guide, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `저장이 취소되면 확인 완료로 처리하지 않는다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage().apply { saveFailure = CancellationException("Save cancelled") }
            val viewModel = createViewModel(storage)
            runCurrent()
            viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide)
            finishPermissionRequests(viewModel)
            runCurrent()

            assertEquals(OnboardingStep.PermissionGuide, viewModel.uiState.value.step)
            assertFalse(storage.confirmed)
            assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
        }

    @Test
    fun `다음을 누르기 전에는 권한을 요청하지 않고 위치 결과 후 알림을 요청한다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage()
            val tracker = FakeAnalyticsTracker()
            val viewModel = createViewModel(storage, tracker)
            runCurrent()

            viewModel.sideEffect.test {
                expectNoEvents()
                repeat(2) { viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide) }
                assertEquals(OnboardingSideEffect.RequestPermission(OnboardingPermission.Location), awaitItem())
                assertFalse(storage.confirmed)
                assertEquals(OnboardingStep.PermissionGuide, viewModel.uiState.value.step)
                expectNoEvents()

                viewModel.dispatch(OnboardingIntent.NotificationPermissionFinished(NotificationPermissionOutcome.Granted))
                runCurrent()
                expectNoEvents()
                assertFalse(storage.confirmed)

                viewModel.dispatch(OnboardingIntent.LocationPermissionFinished(PermissionStatus.Granted))
                assertEquals(OnboardingSideEffect.RequestPermission(OnboardingPermission.Notification), awaitItem())
                assertFalse(storage.confirmed)

                viewModel.dispatch(OnboardingIntent.LocationPermissionFinished(PermissionStatus.Granted))
                runCurrent()
                assertFalse(storage.confirmed)
                expectNoEvents()

                viewModel.dispatch(OnboardingIntent.NotificationPermissionFinished(NotificationPermissionOutcome.Granted))
                runCurrent()
                assertTrue(storage.confirmed)
                assertEquals(OnboardingStep.Guide, viewModel.uiState.value.step)
                assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
                expectNoEvents()
            }
            val results = tracker.events.filter { it.name == "onboarding_permission_result" }.map { it.params() }
            assertEquals(listOf("location", "notification"), results.map { it["permission"] })
            assertEquals(listOf("granted", "newly_granted"), results.map { it["result"] })
            assertEquals(listOf(1L, 1L), results.map { it["granted"] })
        }

    @Test
    fun `권한 안내를 마친 사용자는 권한 요청을 다시 시작하지 않는다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage().apply { confirmed = true }
            val viewModel = createViewModel(storage)
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide)
                runCurrent()
                expectNoEvents()
                assertEquals(OnboardingStep.Guide, viewModel.uiState.value.step)
                assertFalse(viewModel.uiState.value.loadState.isAnyLoading)
            }
        }

    @Test
    fun `온보딩 완료 기록은 권한 안내 기록보다 우선하고 권한을 재요청하지 않는다`() =
        coroutinesTest {
            val storage = FakeOnboardingStorage().apply { completed = true }
            val tracker = FakeAnalyticsTracker()
            val viewModel = createViewModel(storage, tracker)
            runCurrent()

            viewModel.sideEffect.test {
                assertEquals(OnboardingStep.Completed, viewModel.uiState.value.step)
                assertEquals(OnboardingSideEffect.Completed, awaitItem())
                viewModel.dispatch(OnboardingIntent.ConfirmPermissionGuide)
                runCurrent()
                expectNoEvents()
                assertEquals(OnboardingStep.Completed, viewModel.uiState.value.step)
            }
            assertTrue(tracker.events.isEmpty())
        }

    private fun createViewModel(
        storage: OnboardingStorage,
        tracker: FakeAnalyticsTracker = FakeAnalyticsTracker(),
    ): OnboardingViewModel = OnboardingViewModel(storage, tracker)

    private suspend fun finishPermissionRequests(viewModel: OnboardingViewModel) {
        viewModel.sideEffect.test {
            assertEquals(OnboardingSideEffect.RequestPermission(OnboardingPermission.Location), awaitItem())
            viewModel.dispatch(OnboardingIntent.LocationPermissionFinished(PermissionStatus.Granted))
            assertEquals(OnboardingSideEffect.RequestPermission(OnboardingPermission.Notification), awaitItem())
            viewModel.dispatch(OnboardingIntent.NotificationPermissionFinished(NotificationPermissionOutcome.Granted))
        }
    }
}
