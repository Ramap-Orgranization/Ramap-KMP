package com.peto.ramap.ui.shop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.model.review.ShopReviewCache
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.usecase.FeatchShopReviewsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCommunityRepository
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeShopReportRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.lifecycle.ScreenViewModelScope
import com.peto.ramap.ui.main.FakeMapSearchHistoryStorage
import com.peto.ramap.ui.shop.menu.ShopMenuViewModel
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import com.peto.ramap.ui.shop.overview.OverviewViewModel
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.log.ShopAnalytics
import com.peto.ramap.ui.shop.review.ReviewsViewModel
import com.peto.ramap.ui.shop.review.contract.ReviewsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], manifest = org.robolectric.annotation.Config.NONE)
class ShopSheetCompositionTest {
    @Test
    fun `바텀시트 내 탭 재구성에서는 세 ViewModel을 유지하고 닫기와 재열기에서는 모두 새로 만든다`() =
        coroutinesTest {
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(ShopTestApplier(), recomposer)
            val profile = FakeProfileRepository(null)
            val queries = ShopSectionQueries()
            var models: Triple<OverviewViewModel, ShopMenuViewModel, ReviewsViewModel>? = null
            val content: @Composable () -> Unit = { sheet("shop", queries, profile) { models = it } }
            try {
                composition.setContent(content)
                runCurrent()
                val first = requireNotNull(models)
                assertEquals(2, profile.sessionUserIds.subscriptionCount.value)
                composition.setContent(content)
                runCurrent()
                assertSame(first.first, requireNotNull(models).first)
                assertSame(first.second, requireNotNull(models).second)
                assertSame(first.third, requireNotNull(models).third)
                assertEquals(listOf("shop"), queries.menuCalls)
                composition.setContent { }
                runCurrent()
                assertEquals(0, profile.sessionUserIds.subscriptionCount.value)
                queries.menuGate = CompletableDeferred()
                composition.setContent(content)
                runCurrent()
                assertNotSame(first.first, requireNotNull(models).first)
                assertNotSame(first.second, requireNotNull(models).second)
                assertNotSame(first.third, requireNotNull(models).third)
                assertNull(
                    requireNotNull(models)
                        .second.uiState.value.menu,
                )
                queries.menuGate!!.complete(ShopMenu(emptyList()))
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    @Test
    fun `다른 매장으로 바꾸면 이전 수명의 늦은 메뉴 응답은 새 상태를 덮지 않는다`() =
        coroutinesTest {
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(ShopTestApplier(), recomposer)
            val profile = FakeProfileRepository(null)
            val queries = ShopSectionQueries()
            val pending = CompletableDeferred<ShopMenu>()
            queries.menuGate = pending
            var models: Triple<OverviewViewModel, ShopMenuViewModel, ReviewsViewModel>? = null
            try {
                composition.setContent { sheet("first", queries, profile) { models = it } }
                runCurrent()
                val first = requireNotNull(models)
                queries.menuGate = null
                composition.setContent { sheet("second", queries, profile) { models = it } }
                runCurrent()
                assertNotSame(first.first, requireNotNull(models).first)
                assertNotSame(first.second, requireNotNull(models).second)
                assertNotSame(first.third, requireNotNull(models).third)
                pending.complete(ShopMenu(emptyList(), "old"))
                runCurrent()
                assertEquals(
                    "second",
                    requireNotNull(models)
                        .second.uiState.value.shopId,
                )
                assertEquals(
                    "2026-10-14",
                    requireNotNull(models)
                        .second.uiState.value.menu
                        ?.updatedAt,
                )
                assertEquals(2, profile.sessionUserIds.subscriptionCount.value)
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    @Test
    fun `바텀시트를 닫은 뒤 로그인해도 이전 북마크 대기 액션을 실행하지 않는다`() =
        coroutinesTest {
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(ShopTestApplier(), recomposer)
            val login = FakeLoginRepository()
            val personalization = FakePersonalizationRepository()
            val profile = FakeProfileRepository(null)
            var models: Triple<OverviewViewModel, ShopMenuViewModel, ReviewsViewModel>? = null
            try {
                composition.setContent { sheet("shop", ShopSectionQueries(), profile, login, personalization) { models = it } }
                runCurrent()
                requireNotNull(models).first.dispatch(ShopOverviewIntent.OnBookmarkToggled(ramenShopFixture(id = "shop")))
                runCurrent()
                composition.setContent { }
                runCurrent()
                login.updateSessionState(LoginSessionState.AUTHENTICATED)
                personalization.updateBookmarkedShopIds(setOf("existing"))
                runCurrent()
                assertEquals(emptyList(), personalization.bookmarkUpdateRequests)
                assertEquals(0, profile.sessionUserIds.subscriptionCount.value)
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    @Test
    fun `오버뷰 메뉴 리뷰 실패와 재시도는 다른 API와 성공 상태에 영향을 주지 않는다`() =
        coroutinesTest {
            val recomposer = Recomposer(coroutineContext)
            val composition = Composition(ShopTestApplier(), recomposer)
            val queries = ShopSectionQueries()
            val failure = RamapResult.Error(RamapError.Http(500))
            queries.overviewResult = failure
            queries.menuResult = failure
            queries.reviewResult = failure
            var models: Triple<OverviewViewModel, ShopMenuViewModel, ReviewsViewModel>? = null
            try {
                composition.setContent { sheet("shop", queries, FakeProfileRepository(null)) { models = it } }
                runCurrent()
                val (overview, menu, reviews) = requireNotNull(models)
                assertTrue(overview.uiState.value.hasLoadFailure)
                assertTrue(menu.uiState.value.hasLoadFailure)
                assertTrue(reviews.uiState.value.hasReviewLoadFailure)
                queries.overviewResult = null
                overview.dispatch(ShopOverviewIntent.OnOverviewRetry)
                runCurrent()
                assertFalse(overview.uiState.value.hasLoadFailure)
                val overviewData = overview.uiState.value.shopOverview
                assertEquals(2, queries.overviewCalls.size)
                assertEquals(1, queries.menuCalls.size)
                assertEquals(1, queries.reviewCalls.size)
                queries.menuResult = RamapResult.Success(ShopMenu(emptyList()))
                menu.dispatch(ShopMenuIntent.OnRetry)
                runCurrent()
                assertFalse(menu.uiState.value.hasLoadFailure)
                assertEquals(2, queries.overviewCalls.size)
                assertEquals(2, queries.menuCalls.size)
                assertEquals(1, queries.reviewCalls.size)
                queries.reviewResult =
                    RamapResult.Success(
                        com.peto.ramap.domain.model.review
                            .ShopReviewsPage(emptyList(), 0),
                    )
                reviews.dispatch(ReviewsIntent.OnRetry)
                runCurrent()
                assertFalse(reviews.uiState.value.hasReviewLoadFailure)
                assertEquals(2, queries.reviewCalls.size)
                assertEquals(overviewData, overview.uiState.value.shopOverview)
                assertEquals(2, queries.overviewCalls.size)
                assertEquals(2, queries.menuCalls.size)
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    @Composable
    private fun sheet(
        shopId: String,
        queries: ShopSectionQueries,
        profile: FakeProfileRepository,
        login: FakeLoginRepository = FakeLoginRepository(),
        personalization: FakePersonalizationRepository = FakePersonalizationRepository(),
        capture: (Triple<OverviewViewModel, ShopMenuViewModel, ReviewsViewModel>) -> Unit,
    ) {
        ScreenViewModelScope(shopId) {
            val owner = requireNotNull(LocalViewModelStoreOwner.current)
            val reducer = ReviewChangeReducer()
            val changes = FakeReviewChangeStore()
            val factory =
                viewModelFactory {
                    initializer { OverviewViewModel(login, FakeShopReportRepository(), personalization, queries, FakeMapSearchHistoryStorage(), ShopAnalytics(FakeAnalyticsTracker()), SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter())), profile) }
                    initializer { ShopMenuViewModel(queries) }
                    initializer { ReviewsViewModel(queries, FakeCommunityRepository(), profile, login, changes, reducer, FeatchShopReviewsUseCase(queries, changes, reducer, ShopReviewCache(changes, reducer))) }
                }
            val provider = ViewModelProvider.create(owner, factory)
            val overview = provider[OverviewViewModel::class]
            val menu = provider[ShopMenuViewModel::class]
            val reviews = provider[ReviewsViewModel::class]
            capture(Triple(overview, menu, reviews))
            LaunchedEffect(shopId) {
                overview.dispatch(ShopOverviewIntent.OnShopIdSelected(shopId))
                menu.dispatch(ShopMenuIntent.OnShopSelected(shopId))
                reviews.dispatch(ReviewsIntent.OnShopSelected(shopId))
            }
        }
    }
}
