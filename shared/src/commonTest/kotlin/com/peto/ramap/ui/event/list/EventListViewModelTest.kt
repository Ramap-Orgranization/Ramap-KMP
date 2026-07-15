package com.peto.ramap.ui.event.list

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopEventType
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.ui.common.LoadState
import com.peto.ramap.ui.main.event.list.EventListViewModel
import com.peto.ramap.ui.main.event.list.contract.EventListIntent
import com.peto.ramap.ui.main.event.list.contract.EventListSideEffect
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
class EventListViewModelTest {
    @Test
    fun `이벤트를 불러오면 목록을 표시한다`() =
        coroutinesTest {
            val event = event()
            val viewModel =
                EventListViewModel(FakeRamenShopRepository(activeEvents = listOf(event)))

            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()

            assertEquals(LoadState.Content(listOf(event)), viewModel.uiState.value.eventsState)
        }

    @Test
    fun `목록 재진입은 이미 불러온 이벤트를 다시 요청하지 않는다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(activeEvents = listOf(event()))
            val viewModel = EventListViewModel(repository)
            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()

            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()

            assertEquals(1, repository.activeEventsRequestCount)
        }

    @Test
    fun `새로고침 중에는 기존 목록을 유지하고 완료 후 상태를 해제한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = EventListViewModel(repository)
            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()
            repository.activeEventsDelayMillis = 1_000

            viewModel.dispatch(EventListIntent.OnEventListRefreshed)
            runCurrent()

            assertEquals(2, repository.activeEventsRequestCount)
            assertEquals(LoadState.Content(listOf(event)), viewModel.uiState.value.eventsState)
            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `이벤트 조회 실패 후 재시도해도 오류 상태를 표시한다`() =
        coroutinesTest {
            val viewModel =
                EventListViewModel(
                    FakeRamenShopRepository(error = RamapError.Unknown(IllegalStateException("failure"))),
                )
            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()

            viewModel.dispatch(EventListIntent.OnEventListRetried)
            runCurrent()

            assertEquals(LoadState.Error, viewModel.uiState.value.eventsState)
        }

    @Test
    fun `새로고침 실패 시 기존 목록을 유지하고 오류 토스트를 표시한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = EventListViewModel(repository)
            viewModel.dispatch(EventListIntent.OnEventListEntered)
            runCurrent()
            repository.activeEventsError = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(EventListIntent.OnEventListRefreshed)
                runCurrent()

                assertEquals(LoadState.Content(listOf(event)), viewModel.uiState.value.eventsState)
                assertFalse(viewModel.uiState.value.isRefreshing)
                assertEquals(
                    EventListSideEffect.ShowEventListToast(
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
