package com.peto.ramap.ui.main.event

import app.cash.turbine.test
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeNotificationSettingsRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.ShowEventToast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.notification_permission_enable_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {
    @Test
    fun `알림 권한이 거부되면 이벤트 알림을 저장하지 않고 설정 안내를 보여준다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository()
            val viewModel = eventDetailViewModel(repository, permissionGranted = false)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OnNotificationChanged(true))
                runCurrent()

                assertFalse(viewModel.uiState.value.isNotificationEnabled)
                assertEquals(emptyList(), repository.eventNotificationUpdates)
                assertEquals(
                    ShowEventToast(
                        ToastData(
                            message = Res.string.notification_permission_enable_message,
                            type = ToastType.DEFAULT,
                            action = ToastAction(label = Res.string.location_permission_settings_action),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `알림 권한이 허용되면 이벤트 알림을 저장한다`() =
        coroutinesTest {
            val repository = FakeNotificationSettingsRepository()
            val viewModel = eventDetailViewModel(repository, permissionGranted = true)
            viewModel.dispatch(OnEntered(EVENT.id))
            runCurrent()

            viewModel.dispatch(OnNotificationChanged(true))
            runCurrent()

            assertTrue(viewModel.uiState.value.isNotificationEnabled)
            assertEquals(listOf(EVENT.id to true), repository.eventNotificationUpdates)
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
                    permissionGranted = true,
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
        permissionGranted: Boolean,
        ramenShopRepository: FakeRamenShopRepository = FakeRamenShopRepository(activeEvent = EVENT),
    ): EventDetailViewModel =
        EventDetailViewModel(
            ramenShopRepository = ramenShopRepository,
            loginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED),
            notificationRepository = repository,
            requestNotificationPermission = { permissionGranted },
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
