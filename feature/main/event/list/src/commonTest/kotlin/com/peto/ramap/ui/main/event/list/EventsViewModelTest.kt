package com.peto.ramap.ui.main.event.list

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.retry.NetworkRetryGenerator
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
            val viewModel = eventsViewModel(repository)

            runCurrent()

            assertEquals(
                ShopEvents(listOf(event)),
                viewModel.uiState
                    .value
                    .upcomingEvents
                    .single(),
            )
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(EventFilter.EVENT, viewModel.uiState.value.selectedFilter)
            assertEquals(1, repository.activeEventsRequestCount)
        }

    @Test
    fun `새로고침 중에는 기존 목록을 유지하고 완료 후 상태를 해제한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = eventsViewModel(repository)
            runCurrent()
            repository.activeEventsDelayMillis = 1_000

            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()

            assertEquals(2, repository.activeEventsRequestCount)
            assertEquals(
                ShopEvents(listOf(event)),
                viewModel.uiState
                    .value
                    .upcomingEvents
                    .single(),
            )
            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `연속 새로고침은 이전 요청을 교체하고 마지막 요청이 끝날 때 로딩을 해제한다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(activeEventsDelayMillis = 1_000)
            val viewModel = eventsViewModel(repository)
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
                eventsViewModel(
                    FakeRamenShopRepository(error = RamapError.Unknown(IllegalStateException("failure"))),
                )
            runCurrent()

            viewModel.dispatch(EventsIntent.OnEventsRetried)
            runCurrent()

            assertTrue(viewModel.uiState.value.showError)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `네트워크 오류 후 연결이 복구되면 이벤트 목록과 오류 로딩 상태를 복구한다`() =
        coroutinesTest {
            val event = event()
            val repository =
                FakeRamenShopRepository(
                    activeEvents = listOf(event),
                    activeEventsError = RamapError.Network(IllegalStateException("offline")),
                )
            val viewModel = eventsViewModel(repository)
            runCurrent()

            assertTrue(viewModel.uiState.value.showError)
            assertFalse(viewModel.uiState.value.isLoading)

            repository.activeEventsError = null
            repository.activeEventsDelayMillis = 1_000
            NetworkRetryGenerator.retryPending()
            runCurrent()

            assertEquals(2, repository.activeEventsRequestCount)
            assertFalse(viewModel.uiState.value.showError)
            assertTrue(viewModel.uiState.value.isLoading)

            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(
                ShopEvents(listOf(event)),
                viewModel.uiState
                    .value
                    .upcomingEvents
                    .single(),
            )
            assertFalse(viewModel.uiState.value.showError)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `새로고침 실패 시 기존 목록을 유지하고 오류 토스트를 표시한다`() =
        coroutinesTest {
            val event = event()
            val repository = FakeRamenShopRepository(activeEvents = listOf(event))
            val viewModel = eventsViewModel(repository)
            runCurrent()
            repository.activeEventsError = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(EventsIntent.OnEventsRefreshed)
                runCurrent()

                assertEquals(
                    ShopEvents(listOf(event)),
                    viewModel.uiState
                        .value
                        .upcomingEvents
                        .single(),
                )
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

    @Test
    fun `필터 변경은 최초 응답을 사용하고 repository를 다시 호출하지 않는다`() =
        coroutinesTest {
            val repository =
                FakeRamenShopRepository(
                    activeEvents =
                        listOf(
                            event("summer-today", ShopEventType.SUMMER_LIMITED, isToday = true),
                            event("summer-upcoming", ShopEventType.SUMMER_LIMITED),
                            event("new-menu-upcoming", ShopEventType.NEW_MENU),
                            event("renewal-today", ShopEventType.STORE_RENEWAL, isToday = true),
                            event("renewal-upcoming", ShopEventType.STORE_RENEWAL),
                        ),
                )
            val viewModel = eventsViewModel(repository)
            runCurrent()

            assertEquals(1, repository.activeEventsRequestCount)
            assertEquals(
                listOf("summer-today"),
                viewModel.uiState.value.summerLimitedEvents
                    .flatten()
                    .map { it.id },
            )
            assertEquals(
                listOf("summer-upcoming"),
                viewModel.uiState.value.upcomingEvents
                    .flatten()
                    .map { it.id },
            )

            viewModel.dispatch(EventsIntent.OnFilterSelected(EventFilter.NEW_MENU))
            runCurrent()

            assertEquals(1, repository.activeEventsRequestCount)
            assertEquals(
                listOf("new-menu-upcoming"),
                viewModel.uiState.value.upcomingEvents
                    .flatten()
                    .map { it.id },
            )

            viewModel.dispatch(EventsIntent.OnFilterSelected(EventFilter.STORE_RENEWAL))
            runCurrent()

            assertEquals(
                listOf("renewal-today", "renewal-upcoming"),
                (viewModel.uiState.value.ongoingEvents + viewModel.uiState.value.upcomingEvents)
                    .flatten()
                    .map { it.id },
            )
        }

    private fun eventsViewModel(ramenShopRepository: RamenShopRepository): EventsViewModel =
        EventsViewModel(
            ramenShopRepository,
            EventsAnalytics(FakeAnalyticsTracker()),
        )

    private fun event(
        id: String = "event",
        type: ShopEventType = ShopEventType.POPUP,
        isToday: Boolean = false,
    ) = ShopEvent(
        id = id,
        type = type,
        title = "팝업",
        description = "설명",
        startDate = "2026-07-15",
        endDate = "2026-07-16",
        sourceUrl = "https://instagram.com/event",
        isToday = isToday,
        isVenue = true,
        venueShop = ramenShopFixture(id = "shop", name = "매장", address = "서울"),
        waitingMethod = null,
        waitingUrl = null,
    )
}
