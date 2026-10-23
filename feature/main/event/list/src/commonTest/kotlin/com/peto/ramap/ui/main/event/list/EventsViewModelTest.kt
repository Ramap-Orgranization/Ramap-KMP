package com.peto.ramap.ui.main.event.list

import app.cash.turbine.test
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.event.EventVenue
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.event.list.contract.EventsIntent
import com.peto.ramap.ui.main.event.list.contract.EventsSideEffect
import com.peto.ramap.ui.main.event.list.log.EventsAnalytics
import com.peto.ramap.ui.retry.NetworkRetryGenerator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    fun `이벤트를 선택하면 이벤트 유형을 함께 기록한다`() =
        coroutinesTest {
            val analyticsTracker = FakeAnalyticsTracker()
            val viewModel =
                EventsViewModel(
                    FakeRamenShopRepository(),
                    EventsAnalytics(analyticsTracker),
                    FakePersonalizationRepository(),
                    FakeLoginRepository(),
                    signInUseCase(FakeLoginRepository()),
                )

            viewModel.dispatch(EventsIntent.OnEventClicked(event()))
            runCurrent()

            assertEquals(
                mapOf(
                    "content_type" to "event",
                    "event_id" to "event",
                    "event_status" to "upcoming",
                ),
                analyticsTracker.events.single().params(),
            )
        }

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
    fun `조회 중 연속 새로고침은 기존 요청 하나만 유지한다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(activeEventsDelayMillis = 1_000)
            val viewModel = eventsViewModel(repository)
            runCurrent()

            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()
            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            runCurrent()

            assertEquals(1, repository.activeEventsRequestCount)
            assertTrue(viewModel.uiState.value.isLoading)

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
    fun `필터 없이 모든 이벤트 유형을 최초 응답 하나로 표시한다`() =
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
                listOf("summer-upcoming", "new-menu-upcoming", "renewal-upcoming"),
                viewModel.uiState.value.upcomingEvents
                    .flatten()
                    .map { it.id },
            )
            assertEquals(
                listOf("renewal-today"),
                viewModel.uiState.value.ongoingEvents
                    .flatten()
                    .map { it.id },
            )
        }

    @Test
    fun `비로그인 저장 선택은 로그인 안내를 열고 취소하면 검색과 필터를 보존한다`() =
        coroutinesTest {
            val login = FakeLoginRepository()
            val viewModel = EventsViewModel(FakeRamenShopRepository(activeEvents = listOf(event())), EventsAnalytics(FakeAnalyticsTracker()), FakePersonalizationRepository(), login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnQueryChanged("매장"))
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            runCurrent()
            assertTrue(viewModel.uiState.value.showLoginGuide)
            assertFalse(viewModel.uiState.value.savedOnly)
            viewModel.dispatch(EventsIntent.OnLoginDismissed)
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            assertFalse(viewModel.uiState.value.showLoginGuide)
            assertFalse(viewModel.uiState.value.savedOnly)
            assertEquals("매장", viewModel.uiState.value.query)
        }

    @Test
    fun `로그인 결과가 인증보다 먼저 와도 새 개인화 조회가 끝난 후 저장을 한 번 적용한다`() =
        coroutinesTest {
            val login = FakeLoginRepository()
            val delegate = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("previous")))
            var refreshCount = 0
            val store =
                object : PersonalizationStore by delegate {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshCount += 1
                        delay(1_000)
                        delegate.updateBookmarkedShopIds(setOf("shop"))
                        return RamapResult.Success(Unit)
                    }
                }
            val viewModel = EventsViewModel(FakeRamenShopRepository(activeEvents = listOf(event())), EventsAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            assertEquals(0, refreshCount)
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            advanceTimeBy(1_000)
            runCurrent()
            assertTrue(viewModel.uiState.value.savedOnly)
            assertEquals(setOf("shop"), viewModel.uiState.value.bookmarkedShopIds)
            assertEquals(1, refreshCount)
            delegate.updateBookmarkedShopIds(emptySet())
            runCurrent()
            assertTrue(viewModel.uiState.value.savedOnly)
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
        }

    @Test
    fun `로그인 실패나 개인화 대기 중 로그아웃은 저장 재개를 취소한다`() =
        coroutinesTest {
            val login = FakeLoginRepository()
            login.signInResult = RamapResult.Error(RamapError.Unknown(IllegalStateException("cancelled")))
            val delegate = FakePersonalizationRepository()
            val store =
                object : PersonalizationStore by delegate {
                    override suspend fun refresh(): RamapResult<Unit> {
                        delay(1_000)
                        delegate.updateBookmarkedShopIds(setOf("shop"))
                        return RamapResult.Success(Unit)
                    }
                }
            val viewModel = EventsViewModel(FakeRamenShopRepository(activeEvents = listOf(event())), EventsAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            assertFalse(viewModel.uiState.value.showLoginGuide)
            assertFalse(viewModel.uiState.value.savedOnly)
            login.signInResult = RamapResult.Success(Unit)
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            assertTrue(
                viewModel.uiState.value.bookmarkedShopIds
                    .isEmpty(),
            )
        }

    @Test
    fun `검색과 저장은 표시 매장 기준으로 함께 적용하고 새로고침 뒤에도 원본과 조건을 보존한다`() =
        coroutinesTest {
            val first = event("first")
            val other = event("other").copy(venue = EventVenue.Registered(ramenShopFixture(id = "other", name = "다른 매장")))
            val repository = FakeRamenShopRepository(activeEvents = listOf(first, other))
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop")))
            val viewModel = EventsViewModel(repository, EventsAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnQueryChanged(" 다른 "))
            runCurrent()
            assertTrue(viewModel.uiState.value.isEmpty)
            assertEquals(listOf(first, other), viewModel.uiState.value.loadedEvents)
            assertEquals(1, repository.activeEventsRequestCount)
            repository.activeEventsDelayMillis = 1_000
            viewModel.dispatch(EventsIntent.OnEventsRefreshed)
            viewModel.dispatch(EventsIntent.OnQueryChanged(" 매장 "))
            runCurrent()
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(
                listOf("first"),
                viewModel.uiState.value.upcomingEvents
                    .flatten()
                    .map { it.id },
            )
            assertEquals(2, repository.activeEventsRequestCount)
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            runCurrent()
            assertEquals(
                listOf("first", "other"),
                viewModel.uiState.value.upcomingEvents
                    .flatten()
                    .map { it.id },
            )
        }

    @Test
    fun `인증이 로그인 결과보다 먼저 완료돼도 개인화 실패 시 저장을 재개하지 않는다`() =
        coroutinesTest {
            val delegateLogin = FakeLoginRepository()
            val login =
                object : LoginRepository by delegateLogin {
                    override suspend fun signIn(type: LoginType): RamapResult<Unit> {
                        delay(1_000)
                        return RamapResult.Success(Unit)
                    }
                }
            var refreshCount = 0
            var refreshFails = true
            val delegateStore = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("previous")))
            val store =
                object : PersonalizationStore by delegateStore {
                    override suspend fun refresh(): RamapResult<Unit> {
                        refreshCount += 1
                        if (refreshFails) return RamapResult.Error(RamapError.Unknown(IllegalStateException("personalization")))
                        delegateStore.updateBookmarkedShopIds(setOf("shop"))
                        return RamapResult.Success(Unit)
                    }
                }
            val viewModel = EventsViewModel(FakeRamenShopRepository(), EventsAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnLoginSelected(LoginType.APPLE))
            runCurrent()
            delegateLogin.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            assertEquals(0, refreshCount)
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            assertEquals(1, refreshCount)
            assertTrue(viewModel.uiState.value.savedFilterError)
            delegateStore.updateBookmarkedShopIds(setOf("shop"))
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            refreshFails = false
            viewModel.dispatch(EventsIntent.OnPersonalizationRetried)
            runCurrent()
            assertTrue(viewModel.uiState.value.savedOnly)
            assertFalse(viewModel.uiState.value.savedFilterError)
            assertEquals(2, refreshCount)
        }

    @Test
    fun `로그인 후 개인화 대기 중 수동으로 해제한 저장 필터를 늦은 완료가 덮지 않는다`() =
        coroutinesTest {
            val login = FakeLoginRepository()
            val delegate = FakePersonalizationRepository()
            val store =
                object : PersonalizationStore by delegate {
                    override suspend fun refresh(): RamapResult<Unit> {
                        delay(1_000)
                        delegate.updateBookmarkedShopIds(setOf("shop"))
                        return RamapResult.Success(Unit)
                    }
                }
            val viewModel = EventsViewModel(FakeRamenShopRepository(), EventsAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            viewModel.dispatch(EventsIntent.OnSavedToggled)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
        }

    private fun signInUseCase(login: LoginRepository) = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter()))

    private fun eventsViewModel(ramenShopRepository: RamenShopRepository): EventsViewModel =
        EventsViewModel(
            ramenShopRepository,
            EventsAnalytics(FakeAnalyticsTracker()),
            FakePersonalizationRepository(),
            FakeLoginRepository(),
            signInUseCase(FakeLoginRepository()),
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
        venue = EventVenue.Registered(ramenShopFixture(id = "shop", name = "매장", address = "서울")),
        waitingMethod = null,
        waitingUrl = null,
    )
}
