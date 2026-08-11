package com.peto.ramap.ui.main.event.detail

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnRetry
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.ShowToast
import com.peto.ramap.ui.main.event.detail.log.EventDetailAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_notification_load_failure_message
import ramap.shared.generated.resources.event_notification_update_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    @Test
    fun `활성 이벤트가 없으면 이용 불가 안내를 보낸다`() =
        coroutinesTest {
            val viewModel =
                eventDetailViewModel(
                    repository = FakeNotificationSettingsRepository(),
                    ramenShopRepository = FakeRamenShopRepository(),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(OnEntered("missing-event"))
                runCurrent()

                assertEquals(EventUnavailable, awaitItem())
            }
        }

    @Test
    fun `활성 이벤트 조회가 실패하면 화면 내 재시도 상태를 표시한다`() =
        coroutinesTest {
            val viewModel =
                eventDetailViewModel(
                    repository = FakeNotificationSettingsRepository(),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            activeEventError = RamapError.Unknown(IllegalStateException("failure")),
                        ),
                )

            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            assertTrue(viewModel.uiState.value.hasEventLoadFailed)
            assertEquals(null, viewModel.uiState.value.event)
        }

    @Test
    fun `이벤트 조회 실패 후 재시도하면 이벤트를 표시한다`() =
        coroutinesTest {
            val ramenShopRepository =
                FakeRamenShopRepository(
                    activeEvent = EVENT,
                    activeEventError = RamapError.Unknown(IllegalStateException("failure")),
                )
            val viewModel =
                eventDetailViewModel(
                    repository = FakeNotificationSettingsRepository(),
                    ramenShopRepository = ramenShopRepository,
                )
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()
            assertTrue(viewModel.uiState.value.hasEventLoadFailed)

            ramenShopRepository.activeEventError = null
            viewModel.dispatch(OnRetry)
            runCurrent()

            assertFalse(viewModel.uiState.value.hasEventLoadFailed)
            assertEquals(EVENT, viewModel.uiState.value.event)
        }

    @Test
    fun `표시 중인 이벤트 재조회가 실패하면 이전 알림 상태를 모두 제거한다`() =
        coroutinesTest {
            val notificationRepository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(EVENT.id, true)),
                )
            val ramenShopRepository = FakeRamenShopRepository(activeEvent = EVENT)
            val viewModel =
                eventDetailViewModel(
                    repository = notificationRepository,
                    ramenShopRepository = ramenShopRepository,
                )
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()
            assertTrue(viewModel.uiState.value.isNotificationVisible)
            assertTrue(viewModel.uiState.value.canChangeNotification)
            assertTrue(viewModel.uiState.value.isNotificationEnabled)

            ramenShopRepository.activeEventError =
                RamapError.Unknown(IllegalStateException("failure"))
            viewModel.dispatch(OnRetry)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.hasEventLoadFailed)
            assertEquals(null, state.event)
            assertFalse(state.isNotificationVisible)
            assertFalse(state.isEventDayOnly)
            assertFalse(state.canChangeNotification)
            assertFalse(state.isNotificationEnabled)
        }

    @Test
    fun `이벤트 알림을 활성화하면 UI에 권한 요청을 보낸다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository()
            val viewModel = eventDetailViewModel(repository)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnNotificationChanged(true))
                runCurrent()

                assertFalse(viewModel.uiState.value.isNotificationEnabled)
                assertEquals(emptyList(), repository.eventNotificationUpdates)
                assertEquals(RequestNotificationPermission, awaitItem())
            }
        }

    @Test
    fun `UI에서 알림 권한 허용을 전달하면 이벤트 알림을 저장한다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository()
            val viewModel = eventDetailViewModel(repository)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            viewModel.dispatch(OnNotificationPermissionGranted)
            runCurrent()

            assertTrue(viewModel.uiState.value.isNotificationEnabled)
            assertEquals(listOf(EVENT.id to true), repository.eventNotificationUpdates)
        }

    @Test
    fun `이벤트 알림을 비활성화하면 권한 요청 없이 저장한다`() =
        coroutinesTest {
            val repository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(EVENT.id, true)),
                )
            val viewModel = eventDetailViewModel(repository)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            viewModel.dispatch(OnNotificationChanged(false))
            runCurrent()

            assertFalse(viewModel.uiState.value.isNotificationEnabled)
            assertEquals(listOf(EVENT.id to false), repository.eventNotificationUpdates)
        }

    @Test
    fun `같은 이벤트에 재진입하면 이벤트 데이터와 알림 상태를 다시 조회한다`() =
        coroutinesTest {
            val events = mutableListOf(EVENT)
            val repository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(EVENT.id, true)),
                )
            val viewModel =
                eventDetailViewModel(
                    repository = repository,
                    ramenShopRepository = FakeRamenShopRepository(activeEvents = events),
                )
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()
            assertTrue(viewModel.uiState.value.isNotificationEnabled)

            val updatedEvent = EVENT.copy(title = "변경된 제목")
            events[0] = updatedEvent
            repository.eventOverrides[0] = EventNotificationOverride(EVENT.id, false)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            assertFalse(viewModel.uiState.value.isNotificationEnabled)
            assertEquals(updatedEvent, viewModel.uiState.value.event)
            assertEquals(listOf(EVENT.id, EVENT.id), repository.requestedEventNotificationIds)
        }

    @Test
    fun `알림 상태 조회 실패는 초기화된 상태를 유지하고 피드백을 보낸다`() =
        coroutinesTest {
            val repository =
                FakeNotificationSettingsRepository(
                    eventOverrides = mutableListOf(EventNotificationOverride(EVENT.id, true)),
                )
            val viewModel = eventDetailViewModel(repository)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()
            assertTrue(viewModel.uiState.value.isNotificationEnabled)

            repository.eventNotificationStatusError =
                RamapError.Unknown(IllegalStateException("failure"))
            viewModel.sideEffect.test {
                viewModel.dispatch(OnEntered(EVENT.id))
                runCurrent()

                assertFalse(viewModel.uiState.value.isNotificationEnabled)
                assertEquals(
                    ShowToast(Res.string.event_notification_load_failure_message),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `알림 설정 저장 실패는 이전 상태로 복원하고 피드백을 보낸다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository()
            val viewModel = eventDetailViewModel(repository)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()
            repository.eventNotificationUpdateError =
                RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(OnNotificationPermissionGranted)
                runCurrent()

                assertFalse(viewModel.uiState.value.isNotificationEnabled)
                assertEquals(
                    ShowToast(Res.string.event_notification_update_failure_message),
                    awaitItem(),
                )
            }
        }

    private fun eventDetailViewModel(
        repository: FakeNotificationSettingsRepository,
        ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(activeEvent = EVENT),
    ): EventDetailViewModel =
        EventDetailViewModel(
            ramenShopRepository = ramenShopRepository,
            loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
            notificationRepository = repository,
            eventDetailAnalytics = EventDetailAnalytics(FakeAnalyticsTracker()),
        )

    private companion object {
        val EVENT =
            ShopEvent(
                id = "event",
                type = ShopEventType.POPUP,
                title = "팝업",
                description = "설명",
                startDate = "2099-07-15",
                endDate = "2099-07-16",
                sourceUrl = "https://instagram.com/event",
                isToday = false,
                isVenue = true,
                venueShopId = "shop",
                venueShopName = "매장",
                venueAddress = "서울",
                collaboratorShopId = null,
                collaboratorName = null,
                collaboratorInstagramUrl = null,
                waitingMethod = null,
                waitingUrl = null,
            )
    }
}
