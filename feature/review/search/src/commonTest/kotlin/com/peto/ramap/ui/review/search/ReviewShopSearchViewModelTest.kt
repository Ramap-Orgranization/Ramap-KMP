package com.peto.ramap.ui.review.search

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchIntent
import com.peto.ramap.ui.review.search.contract.ReviewShopSearchSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewShopSearchViewModelTest {
    @Test
    fun `연속 입력은 마지막 매장명만 정규화해 검색하고 최근 본 매장 순서를 유지한다`() =
        coroutinesTest {
            val shops = RamenShops(listOf(ramenShopFixture(id = "first"), ramenShopFixture(id = "second")))
            val repository = FakeRamenShopRepository(searchResult = shops, fetchByIdsResult = shops)
            val viewModel = ReviewShopSearchViewModel(SearchRamenShopsUseCase(repository), repository, FakeReviewSearchHistory(listOf("second", "first")))
            runCurrent()
            assertEquals(
                listOf("second", "first"),
                viewModel.uiState.value.shops.keys
                    .toList(),
            )

            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged("라"))
            runCurrent()
            advanceTimeBy(200)
            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged("  RAMEN   SHOP  "))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()

            assertEquals(listOf(SearchQuery("ramen shop")), repository.requestedSearchQueries)
            assertEquals(listOf(50), repository.requestedSearchLimits)
            assertEquals(shops, viewModel.uiState.value.results)
            assertTrue(viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `검색어를 지운 뒤 늦게 도착한 응답은 최근 본 매장 화면을 덮어쓰지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<RamenShops>>()
            val repository =
                object : RamenShopRepository by FakeRamenShopRepository() {
                    override suspend fun searchMapRamenShops(
                        query: SearchQuery,
                        center: Location?,
                        limit: Int,
                    ): RamapResult<RamenShops> = withContext(NonCancellable) { pending.await() }
                }
            val viewModel = ReviewShopSearchViewModel(SearchRamenShopsUseCase(repository), repository, FakeReviewSearchHistory())
            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged("매장"))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged(""))
            runCurrent()
            pending.complete(RamapResult.Success(RamenShops(listOf(ramenShopFixture()))))
            runCurrent()

            assertTrue(viewModel.uiState.value.isShowingHistory)
            assertTrue(
                viewModel.uiState.value.results
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.hasSearched)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `조회 실패는 빈 검색 결과와 구분하고 같은 검색어로 재시도한다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(error = RamapError.Unknown(IllegalStateException("search failed")))
            val viewModel = ReviewShopSearchViewModel(SearchRamenShopsUseCase(repository), repository, FakeReviewSearchHistory())
            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged("매장"))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasError)
            assertFalse(viewModel.uiState.value.hasSearched)

            repository.error = null
            viewModel.dispatch(ReviewShopSearchIntent.Retry)
            runCurrent()

            assertFalse(viewModel.uiState.value.hasError)
            assertTrue(viewModel.uiState.value.hasSearched)
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
        }

    @Test
    fun `현재 검색 결과의 매장만 리뷰 작성으로 열고 기록한다`() =
        coroutinesTest {
            val repository = FakeRamenShopRepository(searchResult = RamenShops(listOf(ramenShopFixture(id = "shop"))))
            val history = FakeReviewSearchHistory()
            val viewModel = ReviewShopSearchViewModel(SearchRamenShopsUseCase(repository), repository, history)
            viewModel.dispatch(ReviewShopSearchIntent.QueryChanged("매장"))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(ReviewShopSearchIntent.SelectShop("stale-shop"))
                runCurrent()
                expectNoEvents()
                viewModel.dispatch(ReviewShopSearchIntent.SelectShop("shop"))
                runCurrent()
                assertEquals(ReviewShopSearchSideEffect.WriteReview("shop"), awaitItem())
                assertEquals(listOf("shop"), history.recentlyViewedShopIds.value)
            }
        }
}
