package com.peto.ramap.ui.onboarding.shopselection

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.ShopRanking
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeOnboardingRankingRepository
import com.peto.ramap.fake.FakeOnboardingStorage
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionSideEffect
import com.peto.ramap.ui.onboarding.shopselection.log.ShopSelectionAnalytics
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShopSelectionPaginationTest {
    private val shops = RamenShops((1..4).map { ramenShopFixture(id = "shop-$it", name = "라멘 $it") })
    private val firstCursor = RankingCursor(likeCount = 10, name = "라멘 2", shopId = "shop-2")
    private val secondCursor = RankingCursor(likeCount = 10, name = "라멘 3", shopId = "shop-3")

    @Test
    fun `커서로 목록을 이어 붙이며 중복 매장과 기존 저장을 유지하고 마지막 페이지에서 멈춘다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-4")))
            val viewModel = createViewModel(ranking = ranking, store = store)
            runCurrent()
            assertEquals(
                listOf("shop-4", "shop-1", "shop-2"),
                viewModel.uiState.value.shops.keys
                    .toList(),
            )
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-1"))
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-2"))
            runCurrent()

            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            assertEquals(
                listOf("shop-4", "shop-1", "shop-2", "shop-3"),
                viewModel.uiState.value.shops.keys
                    .toList(),
            )
            assertEquals(secondCursor, viewModel.uiState.value.nextCursor)
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            assertNull(viewModel.uiState.value.nextCursor)
            assertFalse(viewModel.uiState.value.showsNextPageFooter)
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            viewModel.dispatch(ShopSelectionIntent.RetryNextPage)
            runCurrent()

            assertEquals(listOf(null, firstCursor, secondCursor), ranking.requestedQueries.map { it.cursor })
            assertTrue(ranking.requestedQueries.all { it.limit == 20 && it.areaFilter == AreaFilter.Nationwide && it.categories.isEmpty() })
            assertEquals(4, viewModel.uiState.value.shops.size)
            assertEquals(3, viewModel.uiState.value.bookmarks.savedCount)
            assertTrue(viewModel.uiState.value.canComplete)
        }

    @Test
    fun `추가 로딩 중 중복 요청을 막고 기존 목록에서 계속 저장할 수 있다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val viewModel = createViewModel(ranking = ranking)
            runCurrent()
            val pending = CompletableDeferred<Unit>()
            ranking.pendingRequest = pending
            repeat(3) { viewModel.dispatch(ShopSelectionIntent.LoadNextPage) }
            runCurrent()
            assertEquals(2, ranking.requestedQueries.size)
            assertTrue(viewModel.uiState.value.isLoadingNextPage)
            assertFalse(viewModel.uiState.value.isLoadingShops)
            assertEquals(setOf("shop-1", "shop-2"), viewModel.uiState.value.shops.keys)
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-1"))
            runCurrent()
            assertTrue("shop-1" in viewModel.uiState.value.bookmarks)

            pending.complete(Unit)
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoadingNextPage)
            assertEquals(setOf("shop-1", "shop-2", "shop-3"), viewModel.uiState.value.shops.keys)
        }

    @Test
    fun `추가 조회 실패는 목록과 커서를 유지하고 명시적 재시도로 같은 페이지를 가져온다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val viewModel = createViewModel(ranking = ranking)
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-1"))
            runCurrent()
            ranking.error = RamapError.Unknown(IllegalStateException())
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasNextPageError)
            assertFalse(viewModel.uiState.value.hasShopsError)
            assertFalse(viewModel.uiState.value.isLoadingNextPage)
            assertEquals(firstCursor, viewModel.uiState.value.nextCursor)
            assertEquals(setOf("shop-1", "shop-2"), viewModel.uiState.value.shops.keys)
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            assertEquals(2, ranking.requestedQueries.size)

            ranking.error = null
            viewModel.dispatch(ShopSelectionIntent.RetryNextPage)
            runCurrent()
            assertEquals(listOf(null, firstCursor, firstCursor), ranking.requestedQueries.map { it.cursor })
            assertFalse(viewModel.uiState.value.hasNextPageError)
            assertEquals(setOf("shop-1", "shop-2", "shop-3"), viewModel.uiState.value.shops.keys)
            assertTrue("shop-1" in viewModel.uiState.value.bookmarks)
        }

    @Test
    fun `검색 전환은 추가 조회를 취소하고 검색 해제 후 기존 커서부터 재개한다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val viewModel = createViewModel(ranking = ranking)
            runCurrent()
            val pending = CompletableDeferred<Unit>()
            ranking.pendingRequest = pending
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.QueryChanged("라멘"))
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            advanceUntilIdle()
            pending.complete(Unit)
            runCurrent()
            assertFalse(viewModel.uiState.value.isLoadingNextPage)
            assertFalse(viewModel.uiState.value.showsNextPageFooter)
            assertEquals(setOf("shop-4"), viewModel.uiState.value.shops.keys)
            assertEquals(setOf("shop-1", "shop-2"), viewModel.uiState.value.suggestedShops.keys)
            assertEquals(2, ranking.requestedQueries.size)

            viewModel.dispatch(ShopSelectionIntent.ToggleBookmark("shop-4"))
            runCurrent()
            viewModel.dispatch(ShopSelectionIntent.QueryChanged(""))
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            assertEquals(listOf(null, firstCursor, firstCursor), ranking.requestedQueries.map { it.cursor })
            assertEquals(shops.keys, viewModel.uiState.value.shops.keys)
            assertTrue("shop-4" in viewModel.uiState.value.bookmarks)
        }

    @Test
    fun `첫 페이지 재조회는 진행 중이던 다음 페이지 결과를 무시한다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val viewModel = createViewModel(ranking = ranking)
            runCurrent()
            val pending = CompletableDeferred<Unit>()
            ranking.pendingRequest = pending
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            ranking.pendingRequest = null
            ranking.pages = ranking.pages + (null to page("shop-4"))
            viewModel.dispatch(ShopSelectionIntent.RetryShops)
            runCurrent()
            pending.complete(Unit)
            runCurrent()
            assertEquals(listOf(null, firstCursor, null), ranking.requestedQueries.map { it.cursor })
            assertEquals(setOf("shop-1", "shop-2", "shop-4"), viewModel.uiState.value.shops.keys)
            assertNull(viewModel.uiState.value.nextCursor)
            assertFalse(viewModel.uiState.value.isLoadingNextPage)
        }

    @Test
    fun `온보딩을 완료하면 진행 중인 페이지 조회를 취소하고 추가 요청을 막는다`() =
        coroutinesTest {
            val ranking = pagedRepository()
            val store = FakePersonalizationRepository(Personalization(bookmarkedShopIds = setOf("shop-1", "shop-2", "shop-4")))
            val viewModel = createViewModel(ranking = ranking, store = store)
            runCurrent()
            val pending = CompletableDeferred<Unit>()
            ranking.pendingRequest = pending
            viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
            runCurrent()
            viewModel.sideEffect.test {
                viewModel.dispatch(ShopSelectionIntent.Complete)
                assertEquals(ShopSelectionSideEffect.Completed, awaitItem())
                pending.complete(Unit)
                viewModel.dispatch(ShopSelectionIntent.LoadNextPage)
                runCurrent()
                assertEquals(2, ranking.requestedQueries.size)
                assertFalse(viewModel.uiState.value.isLoadingNextPage)
                assertFalse(viewModel.uiState.value.canLoadNextPage)
                assertFalse("shop-3" in viewModel.uiState.value.shops)
                expectNoEvents()
            }
        }

    private fun pagedRepository(): FakeOnboardingRankingRepository =
        FakeOnboardingRankingRepository(shops).apply {
            pages =
                mapOf(
                    null to page("shop-1", "shop-2", nextCursor = firstCursor),
                    firstCursor to page("shop-2", "shop-3", nextCursor = secondCursor),
                    secondCursor to page("shop-3", "shop-4"),
                )
        }

    private fun page(
        vararg ids: String,
        nextCursor: RankingCursor? = null,
    ): RankingPage = RankingPage(ShopRankings(ids.map { ShopRanking(shops.getValue(it), 10L) }), nextCursor)

    private fun createViewModel(
        ranking: FakeOnboardingRankingRepository,
        store: FakePersonalizationRepository = FakePersonalizationRepository(),
    ): ShopSelectionViewModel {
        val repository =
            FakeRamenShopRepository(
                fetchByIdsResult = shops.filterByShopIds(store.bookmarkedShopIds.value),
                searchResult = shops.filterByShopIds(setOf("shop-4")),
            )
        return ShopSelectionViewModel(
            SearchRamenShopsUseCase(repository),
            repository,
            ranking,
            store,
            FakeLoginRepository(),
            FakeOnboardingStorage(),
            FakeShopSelectionAnalytics(),
        )
    }
}

internal class FakeShopSelectionAnalytics : ShopSelectionAnalytics {
    val events = mutableListOf<String>()

    override fun shopLoadResult(
        operation: String,
        resultCount: Int,
    ) {
        events += "shop_load_result:$operation:$resultCount"
    }

    override fun shopLoadFailed(
        operation: String,
        error: RamapError,
    ) {
        events += "shop_load_failed:$operation"
    }

    override fun bookmarkResult(
        enabled: Boolean,
        savedCount: Int,
        error: RamapError?,
    ) {
        events += "bookmark_result:$enabled:$savedCount"
    }

    override fun finishRequested(savedCount: Int) {
        events += "finish_requested:$savedCount"
    }

    override fun finishFailed(savedCount: Int) {
        events += "finish_failed:$savedCount"
    }

    override fun completed(savedCount: Int) {
        events += "completed:$savedCount"
    }
}
