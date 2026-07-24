package com.peto.ramap.ui.main.event

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
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.RequestNotificationPermission
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
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
    fun `활성 이벤트 조회가 실패하면 이용 불가 안내를 보낸다`() =
        coroutinesTest {
            val viewModel =
                eventDetailViewModel(
                    repository = FakeNotificationSettingsRepository(),
                    ramenShopRepository =
                        FakeRamenShopRepository(
                            activeEventError = RamapError.Unknown(IllegalStateException("failure")),
                        ),
                )

            viewModel.sideEffect.test {
                viewModel.dispatch(OnEntered(EVENT.id))
                runCurrent()

                assertEquals(EventUnavailable, awaitItem())
            }
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

    private fun eventDetailViewModel(
        repository: FakeNotificationSettingsRepository,
        ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(activeEvent = EVENT),
    ): EventDetailViewModel =
        EventDetailViewModel(
            ramenShopRepository = ramenShopRepository,
            loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
            notificationRepository = repository,
            analyticsTracker = FakeAnalyticsTracker(),
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
