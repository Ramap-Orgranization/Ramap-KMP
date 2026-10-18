package com.peto.ramap.ui.main.notice

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOperatingNoticeRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import com.peto.ramap.ui.main.notice.log.OperatingNoticeAnalytics
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            val viewModel = OperatingNoticeViewModel(FakeOperatingNoticeRepository(notices = listOf(operatingNotice())), OperatingNoticeAnalytics(FakeAnalyticsTracker()), store, login)
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
        )
}
