package com.peto.ramap.ui.event.list

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.ui.common.RamapUiState
import com.peto.ramap.ui.main.event.list.EventsViewModel
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {
    @Test
    fun `이벤트를 불러오면 목록을 표시한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = EventsViewModel(repository)

            runCurrent()

            assertEquals(RamapUiState.Success(listOf(event)), viewModel.uiState.value.eventsState)
            assertEquals(1, repository.activeEventsRequestCount)
        }

    @Test
    fun `새로고침 중에는 기존 목록을 유지하고 완료 후 상태를 해제한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = EventsViewModel(repository)
            runCurrent()
            repository.activeEventsDelayMillis = 1_000

            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()

            assertEquals(2, repository.activeEventsRequestCount)
            assertEquals(RamapUiState.Success(listOf(event)), viewModel.uiState.value.eventsState)
            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `연속 새로고침은 이전 요청을 교체하고 마지막 요청이 끝날 때 로딩을 해제한다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(activeEventsDelayMillis = 1_000)
            val viewModel = EventsViewModel(repository)
            runCurrent()

            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()
            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()

            assertEquals(3, repository.activeEventsRequestCount)
            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `이벤트 조회 실패 후 재시도해도 오류 상태를 표시한다`() =
        coroutinesTest {
            val viewModel =
                EventsViewModel(
                    FakeRamenShopRepository(error = RamapError.Unknown(IllegalStateException("failure"))),
                )
            runCurrent()

            viewModel.dispatch(EventsIntent.OnEventsRetried)
            runCurrent()

            assertEquals(RamapUiState.Error, viewModel.uiState.value.eventsState)
        }

    @Test
    fun `새로고침 실패 시 기존 목록을 유지하고 오류 토스트를 표시한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = EventsViewModel(repository)
            runCurrent()
            repository.activeEventsError = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(EventsIntent.OnEventsRefreshed)
                runCurrent()

                assertEquals(RamapUiState.Success(listOf(event)), viewModel.uiState.value.eventsState)
                assertFalse(viewModel.uiState.value.isRefreshing)
                assertEquals(
                    EventsSideEffect.ShowEventsToast(
                        ToastData(
                            message = Res.string.event_list_refresh_failure_message,
                            type = ToastType.ERROR,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    private fun event() =
        ShopEvent(
            id = "event",
            type = ShopEventType.POPUP,
            title = "팝업",
            description = "설명",
            startDate = "2026-07-15",
            endDate = "2026-07-16",
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
