package com.peto.ramap.ui.main.notice

import app.cash.turbine.test
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import com.peto.ramap.ui.main.notice.log.OperatingNoticeAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class OperatingNoticeViewModelTest {
    @Test
    fun `영업 공지를 선택하면 유형과 공지 식별자를 기록한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val analyticsTracker = FakeAnalyticsTracker()
            val viewModel =
                OperatingNoticeViewModel(
                    FakeOperatingNoticeRepository(),
                    OperatingNoticeAnalytics(analyticsTracker),
                    FakePersonalizationRepository(),
                    FakeLoginRepository(),
                    signInUseCase(FakeLoginRepository()),
                )

            viewModel.dispatch(OperatingNoticeIntent.OnNoticeClicked(notice))
            runCurrent()

            assertEquals("operating_notice_select", analyticsTracker.events.single().name)
            assertEquals(
                mapOf(
                    "content_type" to "operating_notice",
                    "operating_notice_id" to notice.id,
                ),
                analyticsTracker.events.single().params(),
            )
        }

    @Test
    fun `영업 공지를 불러오면 목록을 표시한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val repository = FakeOperatingNoticeRepository(notices = listOf(notice))
            val viewModel = operatingNoticeViewModel(repository)

            runCurrent()

            assertEquals(listOf(notice), viewModel.uiState.value.todayOperatingNotices)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.fetchCount)
        }

    @Test
    fun `새로고침 중에는 기존 목록을 유지하고 완료 후 상태를 해제한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val repository = FakeOperatingNoticeRepository(notices = listOf(notice))
            val viewModel = operatingNoticeViewModel(repository)
            runCurrent()
            repository.delayMillis = 1_000

            viewModel.dispatch(OperatingNoticeIntent.OnRefreshed)
            runCurrent()

            assertEquals(2, repository.fetchCount)
            assertEquals(listOf(notice), viewModel.uiState.value.todayOperatingNotices)
            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceTimeBy(1_000)
            runCurrent()

            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `새로고침 실패 시 기존 목록을 유지하고 오류 토스트를 표시한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val repository = FakeOperatingNoticeRepository(notices = listOf(notice))
            val viewModel = operatingNoticeViewModel(repository)
            runCurrent()
            repository.error = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(OperatingNoticeIntent.OnRefreshed)
                runCurrent()

                assertEquals(listOf(notice), viewModel.uiState.value.todayOperatingNotices)
                assertFalse(viewModel.uiState.value.isRefreshing)
                assertEquals(
                    OperatingNoticeSideEffect.ShowToast(
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
    fun `오늘 공지와 미래 공지를 각각 분리한다`() =
        coroutinesTest {
            val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))
            val current = operatingNotice(id = "today", startDate = today, endDate = today)
            val scheduled = operatingNotice(id = "future", startDate = today.plus(1, DateTimeUnit.DAY), endDate = today.plus(1, DateTimeUnit.DAY))
            val viewModel = operatingNoticeViewModel(FakeOperatingNoticeRepository(notices = listOf(current, scheduled)))

            runCurrent()

            assertEquals(listOf(current), viewModel.uiState.value.todayOperatingNotices)
            assertEquals(listOf(scheduled), viewModel.uiState.value.scheduledOperatingNotices)
        }

    @Test
    fun `검색 입력은 진행 중 새로고침 완료 뒤에도 최신 조건으로 집계한다`() =
        coroutinesTest {
            val repository = FakeOperatingNoticeRepository(notices = listOf(operatingNotice(), operatingNotice("other").copy(shop = ramenShopFixture(id = "other", name = "다른 매장"))))
            val viewModel = operatingNoticeViewModel(repository)
            runCurrent()
            repository.delayMillis = 1_000
            viewModel.dispatch(OperatingNoticeIntent.OnRefreshed)
            viewModel.dispatch(OperatingNoticeIntent.OnQueryChanged("다른"))
            viewModel.dispatch(OperatingNoticeIntent.OnQueryChanged(" 매장 "))
            runCurrent()
            assertEquals(2, viewModel.uiState.value.todayShops.size)
            viewModel.dispatch(OperatingNoticeIntent.OnQueryChanged("다른"))
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(
                listOf("other"),
                viewModel.uiState.value.todayShops
                    .map { it.shop.id },
            )
        }

    @Test
    fun `저장 필터의 초기 로딩 실패를 빈 결과와 구분하고 저장 변경과 로그아웃을 반영한다`() =
        coroutinesTest {
            val store = FakePersonalizationRepository(initialState = PersonalizationBootstrapState.Loading)
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(notices = listOf(operatingNotice())), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            runCurrent()
            assertTrue(viewModel.uiState.value.savedOnly)
            assertEquals(PersonalizationBootstrapState.Loading, viewModel.uiState.value.personalizationState)
            store.updateBootstrapState(PersonalizationBootstrapState.Error)
            runCurrent()
            assertEquals(PersonalizationBootstrapState.Error, viewModel.uiState.value.personalizationState)
            store.updateBookmarkedShopIds(setOf("shop"))
            runCurrent()
            assertEquals(1, viewModel.uiState.value.todayShops.size)
            store.updateBookmarkedShopIds(emptySet())
            runCurrent()
            assertEquals(0, viewModel.uiState.value.todayShops.size)
            login.updateSessionState(LoginSessionState.NOT_AUTHENTICATED)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            assertTrue(
                viewModel.uiState.value.bookmarkedShopIds
                    .isEmpty(),
            )
            store.clear()
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.bookmarkedShopIds
                    .isEmpty(),
            )
        }

    @Test
    fun `비로그인 저장 선택은 로그인 안내를 열고 취소하면 검색과 필터를 보존한다`() =
        coroutinesTest {
            val login = FakeLoginRepository()
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(), OperatingNoticeAnalytics(FakeAnalyticsTracker()), FakePersonalizationRepository(), login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnQueryChanged("매장"))
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            runCurrent()
            assertTrue(viewModel.uiState.value.showLoginGuide)
            assertFalse(viewModel.uiState.value.savedOnly)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginDismissed)
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
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(LoginType.KAKAO))
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
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
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
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            assertFalse(viewModel.uiState.value.showLoginGuide)
            assertFalse(viewModel.uiState.value.savedOnly)
            login.signInResult = RamapResult.Success(Unit)
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(LoginType.KAKAO))
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
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(LoginType.APPLE))
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
            viewModel.dispatch(OperatingNoticeIntent.OnPersonalizationRetried)
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
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login, signInUseCase(login))
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(LoginType.KAKAO))
            runCurrent()
            login.updateSessionState(LoginSessionState.AUTHENTICATED)
            runCurrent()
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
            advanceTimeBy(1_000)
            runCurrent()
            assertFalse(viewModel.uiState.value.savedOnly)
        }

    private fun signInUseCase(login: LoginRepository) = SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter()))

    private fun operatingNotice(
        id: String = "notice",
        startDate: LocalDate = Clock.System.todayIn(TimeZone.of("Asia/Seoul")),
        endDate: LocalDate? = startDate,
    ) = OperatingNotice(
        id = id,
        shop = ramenShopFixture(id = "shop", name = "매장", address = "서울"),
        type = OperatingNoticeType.TEMPORARY_CLOSURE,
        description = "내부 사정으로 쉽니다.",
        startDate = startDate,
        endDate = endDate,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )

    private fun operatingNoticeViewModel(repository: FakeOperatingNoticeRepository) =
        OperatingNoticeViewModel(
            repository,
            OperatingNoticeAnalytics(FakeAnalyticsTracker()),
            FakePersonalizationRepository(),
            FakeLoginRepository(),
            signInUseCase(FakeLoginRepository()),
        )
}
