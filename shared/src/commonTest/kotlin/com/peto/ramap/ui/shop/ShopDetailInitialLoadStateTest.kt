package com.peto.ramap.ui.shop

import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.shop.menu.contract.ShopMenuLoadKey
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ShopDetailDisplayState
import com.peto.ramap.ui.shop.model.ShopDetailInitialLoadState
import com.peto.ramap.ui.shop.model.ShopOverviewUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewLoadKey
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.review.contract.ReviewsLoadKey
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopDetailInitialLoadStateTest {
    @Test
    fun `요청 시작 전 로딩 키가 없어도 전체 스켈레톤을 표시한다`() {
        val overview = ShopOverviewState()
        val progress = ShopDetailInitialLoadState("shop").update(overview, ShopMenuUiState(), ReviewsUiState())
        assertEquals(ShopDetailDisplayState.InitialSkeleton, progress.displayState(overview))
    }

    @Test
    fun `세 최초 조회는 어떤 순서로 끝나도 마지막 응답까지 전체 스켈레톤을 유지한다`() {
        val orders = listOf(listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2), listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0))
        for (order in orders) {
            var overview = loadingOverview()
            var menu = loadingMenu()
            var reviews = loadingReviews()
            var progress = ShopDetailInitialLoadState("shop")
            for ((index, section) in order.withIndex()) {
                when (section) {
                    0 -> overview = successfulOverview()
                    1 -> menu = successfulMenu()
                    2 -> reviews = successfulReviews()
                }
                progress = progress.update(overview, menu, reviews)
                assertEquals(
                    if (index == order.lastIndex) ShopDetailDisplayState.Content else ShopDetailDisplayState.InitialSkeleton,
                    progress.displayState(overview),
                    "조회 완료 순서 $order 에서 $index 단계 표시",
                )
            }
        }
    }

    @Test
    fun `메뉴와 리뷰가 비어 있는 성공 응답도 완료로 처리한다`() {
        val overview = successfulOverview()
        val progress = ShopDetailInitialLoadState("shop").update(overview, successfulMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
    }

    @Test
    fun `메뉴 리뷰 실패도 완료이며 오버뷰가 성공하면 상세 내용을 표시한다`() {
        val overview = successfulOverview()
        val progress = ShopDetailInitialLoadState("shop").update(overview, failedMenu(), failedReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
    }

    @Test
    fun `오버뷰 실패도 다른 최초 조회를 기다린 뒤 전체 오류를 표시한다`() {
        val overview = failedOverview()
        var progress = ShopDetailInitialLoadState("shop").update(overview, successfulMenu(), loadingReviews())
        assertEquals(ShopDetailDisplayState.InitialSkeleton, progress.displayState(overview))
        progress = progress.update(overview, successfulMenu(), failedReviews())
        assertEquals(ShopDetailDisplayState.OverviewError, progress.displayState(overview))
    }

    @Test
    fun `최초 완료 후 메뉴 리뷰 재시도와 리뷰 추가 로딩은 오버뷰를 가리지 않는다`() {
        val overview = successfulOverview()
        var progress = ShopDetailInitialLoadState("shop").update(overview, failedMenu(), failedReviews())
        progress = progress.update(overview, loadingMenu(), loadingReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
        progress = progress.update(overview, successfulMenu(), successfulReviews().copy(loadState = LoadState.loading(ReviewsLoadKey.ReviewsPage)))
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
        progress = progress.update(overview, successfulMenu(), loadingReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
    }

    @Test
    fun `전체 오류에서 오버뷰만 재시도하면 전체 스켈레톤 없이 로딩 후 상세를 표시한다`() {
        var overview = failedOverview()
        var progress = ShopDetailInitialLoadState("shop").update(overview, failedMenu(), failedReviews())
        overview = loadingOverview()
        progress = progress.update(overview, failedMenu(), failedReviews())
        assertEquals(ShopDetailDisplayState.OverviewLoading, progress.displayState(overview))
        overview = failedOverview()
        progress = progress.update(overview, failedMenu(), failedReviews())
        assertEquals(ShopDetailDisplayState.OverviewError, progress.displayState(overview))
        overview = successfulOverview()
        progress = progress.update(overview, failedMenu(), failedReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
    }

    @Test
    fun `이미 표시한 오버뷰의 갱신과 실패는 기존 내용을 유지한다`() {
        var overview = successfulOverview()
        var progress = ShopDetailInitialLoadState("shop").update(overview, successfulMenu(), successfulReviews())
        overview = overview.copy(loadState = LoadState.loading(ShopOverviewLoadKey.Overview))
        progress = progress.update(overview, successfulMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
        overview = overview.copy(loadState = LoadState(), hasLoadFailure = true)
        progress = progress.update(overview, successfulMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
        overview = failedOverview().copy(isShopUnavailable = true)
        assertEquals(ShopDetailDisplayState.OverviewError, progress.displayState(overview))
    }

    @Test
    fun `최초 캐시가 있어도 진행 중 요청은 완료로 판단하지 않고 실패하면 전체 오류를 표시한다`() {
        var overview = successfulOverview().copy(loadState = LoadState.loading(ShopOverviewLoadKey.Overview))
        var menu = successfulMenu().copy(loadState = LoadState.loading(ShopMenuLoadKey.Menu))
        var reviews = successfulReviews().copy(loadState = LoadState.loading(ReviewsLoadKey.Reviews))
        var progress = ShopDetailInitialLoadState("shop").update(overview, menu, reviews)
        assertEquals(ShopDetailDisplayState.InitialSkeleton, progress.displayState(overview))
        overview = overview.copy(loadState = LoadState(), hasLoadFailure = true)
        menu = menu.copy(loadState = LoadState())
        reviews = reviews.copy(loadState = LoadState())
        progress = progress.update(overview, menu, reviews)
        assertEquals(ShopDetailDisplayState.OverviewError, progress.displayState(overview))
        overview = overview.copy(loadState = LoadState.loading(ShopOverviewLoadKey.Overview), hasLoadFailure = false)
        progress = progress.update(overview, menu, reviews)
        assertEquals(ShopDetailDisplayState.OverviewLoading, progress.displayState(overview))
    }

    @Test
    fun `매장 전환 또는 재열기에서는 이전 매장 완료 상태를 사용하지 않는다`() {
        val overview = successfulOverview()
        val previous = ShopDetailInitialLoadState("shop").update(overview, successfulMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.Content, previous.displayState(overview))
        val next = ShopDetailInitialLoadState("next").update(overview, successfulMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.InitialSkeleton, next.displayState(overview))
        val reopened = ShopDetailInitialLoadState("shop").update(loadingOverview(), loadingMenu(), loadingReviews())
        assertEquals(ShopDetailDisplayState.InitialSkeleton, reopened.displayState(loadingOverview()))
    }

    @Test
    fun `먼저 끝난 메뉴를 재시도해도 나머지 최초 조회가 끝나면 스켈레톤을 해제한다`() {
        var progress = ShopDetailInitialLoadState("shop").update(loadingOverview(), successfulMenu(), loadingReviews())
        val overview = successfulOverview()
        progress = progress.update(overview, loadingMenu(), successfulReviews())
        assertEquals(ShopDetailDisplayState.Content, progress.displayState(overview))
    }

    private fun successfulOverview() = ShopOverviewState(overviewState = ShopOverviewUiState.Content(ShopOverView(ramenShopFixture(id = "shop"), 0L, null, null, null)))

    private fun loadingOverview() = ShopOverviewState(overviewState = ShopOverviewUiState.Loading("shop", null), loadState = LoadState.loading(ShopOverviewLoadKey.Overview))

    private fun failedOverview() = ShopOverviewState(overviewState = ShopOverviewUiState.Error("shop", null), hasLoadFailure = true)

    private fun successfulMenu() = ShopMenuUiState(shopId = "shop", menu = ShopMenu(emptyList()))

    private fun loadingMenu() = ShopMenuUiState(shopId = "shop", loadState = LoadState.loading(ShopMenuLoadKey.Menu))

    private fun failedMenu() = ShopMenuUiState(shopId = "shop", hasLoadFailure = true)

    private fun successfulReviews() = ReviewsUiState(shopId = "shop", hasLoaded = true)

    private fun loadingReviews() = ReviewsUiState(shopId = "shop", loadState = LoadState.loading(ReviewsLoadKey.Reviews))

    private fun failedReviews() = ReviewsUiState(shopId = "shop", hasReviewLoadFailure = true)
}
