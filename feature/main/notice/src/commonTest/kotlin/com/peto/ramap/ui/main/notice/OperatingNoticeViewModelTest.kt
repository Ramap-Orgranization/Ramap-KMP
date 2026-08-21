package com.peto.ramap.ui.main.notice

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.repository.OperatingNoticeRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.datetime.LocalDate
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_list_refresh_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OperatingNoticeViewModelTest {
    @Test
    fun `영업 공지를 불러오면 목록을 표시한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val repository = FakeOperatingNoticeRepository(notices = listOf(notice))
            val viewModel = OperatingNoticeViewModel(repository)

            runCurrent()

            assertEquals(listOf(notice), viewModel.uiState.value.operatingNotices)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, repository.fetchCount)
        }

    @Test
    fun `새로고침 중에는 기존 목록을 유지하고 완료 후 상태를 해제한다`() =
        coroutinesTest {
            val notice = operatingNotice()
            val repository = FakeOperatingNoticeRepository(notices = listOf(notice))
            val viewModel = OperatingNoticeViewModel(repository)
            runCurrent()
            repository.delayMillis = 1_000

            viewModel.dispatch(OperatingNoticeIntent.OnRefreshed)
            runCurrent()

            assertEquals(2, repository.fetchCount)
            assertEquals(listOf(notice), viewModel.uiState.value.operatingNotices)
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
            val viewModel = OperatingNoticeViewModel(repository)
            runCurrent()
            repository.error = RamapError.Unknown(IllegalStateException("failure"))

            viewModel.sideEffect.test {
                viewModel.dispatch(OperatingNoticeIntent.OnRefreshed)
                runCurrent()

                assertEquals(listOf(notice), viewModel.uiState.value.operatingNotices)
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

    private fun operatingNotice(id: String = "notice") =
        OperatingNotice(
            id = id,
            shop = ramenShopFixture(id = "shop", name = "매장", address = "서울"),
            type = OperatingNoticeType.TEMPORARY_CLOSURE,
            description = "내부 사정으로 쉽니다.",
            startDate = LocalDate(2026, 8, 21),
            endDate = LocalDate(2026, 8, 21),
            startTime = null,
            endTime = null,
            sourceUrl = null,
        )

    private class FakeOperatingNoticeRepository(
        var notices: List<OperatingNotice> = emptyList(),
        var error: RamapError? = null,
        var delayMillis: Long = 0,
    ) : OperatingNoticeRepository {
        var fetchCount = 0
            private set

        override suspend fun fetchCurrentOperatingNotices(): RamapResult<List<OperatingNotice>> {
            fetchCount++
            if (delayMillis > 0) kotlinx.coroutines.delay(delayMillis)
            return error?.let { RamapResult.Error(it) } ?: RamapResult.Success(notices)
        }
    }
}
