package com.peto.ramap.ui.main.newshops

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpening
import com.peto.ramap.ui.main.newshops.contract.NewShopsIntent
import com.peto.ramap.ui.main.newshops.contract.NewShopsSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NewShopsViewModelTest {
    @Test
    fun `이번 달에 오픈한 매장 목록과 매장 수를 제공한다`() =
        coroutinesTest {
            val first = shop("first", "첫 매장")
            val other = shop("other", "다른 매장")
            val viewModel = NewShopsViewModel(FakeNewShopsRepository { RamapResult.Success(listOf(first, other)) }, novemberClock())

            runCurrent()

            assertEquals(11, viewModel.uiState.value.month.monthNumber)
            assertEquals(2, viewModel.uiState.value.totalShopCount)
            assertEquals(listOf(first, other), viewModel.uiState.value.shops)
        }

    @Test
    fun `월이 바뀌면 기존 목록을 비우고 취소된 조회의 응답을 표시하지 않는다`() =
        coroutinesTest {
            val oldShop = shop("past", "지난달 매장")
            val newShop = shop("present", "이번 달 매장", LocalDate(2026, 12, 1))
            val oldRefresh = CompletableDeferred<RamapResult<List<NewShop>>>()
            val newMonth = CompletableDeferred<RamapResult<List<NewShop>>>()
            val clock = novemberClock()
            var initialFetch = true
            val repository =
                FakeNewShopsRepository { month ->
                    when {
                        month.monthNumber == 12 -> newMonth.await()
                        initialFetch -> {
                            initialFetch = false
                            RamapResult.Success(listOf(oldShop))
                        }

                        else -> withContext(NonCancellable) { oldRefresh.await() }
                    }
                }
            val viewModel = NewShopsViewModel(repository, clock)
            runCurrent()
            assertEquals(listOf(oldShop), viewModel.uiState.value.shops)
            viewModel.dispatch(NewShopsIntent.OnRefreshRequested(viewModel.currentOpeningMonth()))
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            clock.instant = Instant.parse("2026-11-30T15:00:00Z")
            val refreshedMonth = viewModel.currentOpeningMonth()
            val viewBeforeRefresh = viewModel.uiState.value.visibleIn(refreshedMonth)
            assertNull(viewBeforeRefresh.shops)
            viewModel.dispatch(NewShopsIntent.OnShown(refreshedMonth))
            runCurrent()

            assertEquals(12, viewModel.uiState.value.month.monthNumber)
            assertTrue(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertNull(viewModel.uiState.value.shops)
            newMonth.complete(RamapResult.Success(listOf(newShop)))
            runCurrent()
            oldRefresh.complete(RamapResult.Success(listOf(oldShop)))
            runCurrent()

            assertEquals(listOf(newShop), viewModel.uiState.value.shops)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `첫 조회 실패는 재시도할 수 있고 새로고침 실패는 기존 매장과 오류 안내를 보여 준다`() =
        coroutinesTest {
            val shop = shop("first", "첫 매장")
            val error = RamapResult.Error(RamapError.Http(503))
            val results = ArrayDeque<RamapResult<List<NewShop>>>(listOf(error, RamapResult.Success(listOf(shop)), error))
            val viewModel = NewShopsViewModel(FakeNewShopsRepository { results.removeFirst() }, novemberClock())

            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertNull(viewModel.uiState.value.shops)
            viewModel.dispatch(NewShopsIntent.OnRefreshRequested(viewModel.currentOpeningMonth()))
            runCurrent()
            assertEquals(listOf(shop), viewModel.uiState.value.shops)
            assertFalse(viewModel.uiState.value.failed)

            viewModel.sideEffect.test {
                viewModel.dispatch(NewShopsIntent.OnRefreshRequested(viewModel.currentOpeningMonth()))
                runCurrent()
                assertEquals(NewShopsSideEffect.ShowRefreshFailure, awaitItem())
                assertEquals(listOf(shop), viewModel.uiState.value.shops)
                assertFalse(viewModel.uiState.value.failed)
                assertFalse(viewModel.uiState.value.isRefreshing)
            }
        }

    private fun shop(
        id: String,
        name: String,
        openingDate: LocalDate = LocalDate(2026, 11, 9),
    ): NewShop =
        NewShop(
            id = id,
            name = name,
            opening = ShopOpening(openingDate, ShopOpening.Kind.OPENING),
            coverImageUrl = null,
        )

    private fun novemberClock(): NewShopsTestClock = NewShopsTestClock(Instant.parse("2026-11-14T00:00:00Z"))
}
