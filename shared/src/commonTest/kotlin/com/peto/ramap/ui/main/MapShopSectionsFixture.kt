package com.peto.ramap.ui.main

import androidx.lifecycle.ViewModelStore
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.repository.CommunityRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.domain.repository.ShopMenuRepository
import com.peto.ramap.domain.repository.ShopReportRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.domain.usecase.FetchShopOverviewUseCase
import com.peto.ramap.domain.usecase.LoadShopReviewsUseCase
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.fake.FakeAnalyticsTracker
import com.peto.ramap.fake.FakeCrashReporter
import com.peto.ramap.ui.main.map.MapViewModel
import com.peto.ramap.ui.main.map.contract.MapIntent
import com.peto.ramap.ui.main.map.contract.MapSideEffect
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.log.ShopAnalytics
import com.peto.ramap.ui.shop.menu.ShopMenuViewModel
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import com.peto.ramap.ui.shop.overview.ShopOverviewViewModel
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.review.ShopReviewViewModel
import com.peto.ramap.ui.shop.review.contract.ShopReviewIntent
import com.peto.ramap.ui.shop.review.contract.ShopReviewSideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 실제 ViewModel 세 개와 지도 콜백을 함께 검증하는 테스트 전용 바텀시트. */
class MapShopSectionsFixture(
    private val scope: CoroutineScope,
    private val map: MapViewModel,
    private val login: LoginRepository,
    private val reports: ShopReportRepository,
    private val personalization: PersonalizationStore,
    private val overviewQuery: FetchShopOverviewUseCase,
    private val profile: ProfileRepository,
    private val reviews: ReviewRepository,
    private val community: CommunityRepository,
    private val changes: ReviewChangeStore,
    private val menus: ShopMenuRepository,
) {
    val uiState = MutableStateFlow(ShopSectionsSnapshot(map.uiState.value))
    private val effects = Channel<ShopInteractionEffect>(Channel.BUFFERED)
    val sideEffect = effects.receiveAsFlow()
    private var store: ViewModelStore? = null
    private var collectors: List<Job> = emptyList()
    private var overview: ShopOverviewViewModel? = null
    private var menu: ShopMenuViewModel? = null
    private var review: ShopReviewViewModel? = null
    private var mapLogin = false

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            map.uiState.map { it.shopSelectionKey to it.selectedShopId }.distinctUntilChanged().collect { (_, id) ->
                close()
                if (id != null) open(id)
            }
        }
        scope.launch { map.uiState.collect { state -> uiState.value = uiState.value.copy(map = state) } }
        scope.launch { map.sideEffect.collect { if (it is MapSideEffect.Interaction) effects.send(it.effect) } }
    }

    private fun open(shopId: String?) {
        val overviewVm = ShopOverviewViewModel(login, reports, personalization, overviewQuery, FakeMapSearchHistoryStorage(), ShopAnalytics(FakeAnalyticsTracker()), SignInUseCase(login, LoginAnalytics(FakeAnalyticsTracker(), FakeCrashReporter())), profile)
        val menuVm = ShopMenuViewModel(menus)
        val reducer = ReviewChangeReducer()
        val reviewVm = ShopReviewViewModel(reviews, community, profile, login, changes, reducer, LoadShopReviewsUseCase(reviews, changes, reducer))
        overview = overviewVm
        menu = menuVm
        review = reviewVm
        store =
            ViewModelStore().apply {
                put("overview", overviewVm)
                put("menu", menuVm)
                put("review", reviewVm)
            }
        collectors =
            listOf(
                scope.launch {
                    combine(overviewVm.uiState, menuVm.uiState, reviewVm.uiState) { a, b, c -> Triple(a, b, c) }.collect { (a, b, c) ->
                        uiState.value = uiState.value.copy(overview = a, menuState = b, reviewState = c)
                        if (shopId != null && a.hasLoadFailure && a.shopOverview == null) map.dispatch(MapIntent.OnShopLoadFailed(shopId))
                    }
                },
                scope.launch {
                    overviewVm.sideEffect.collect { effect ->
                        when (effect) {
                            is ShopOverviewSideEffect.Interaction -> effects.send(effect.effect)
                            is ShopOverviewSideEffect.ShopViewed -> map.dispatch(MapIntent.OnShopViewed(effect.shop))
                            ShopOverviewSideEffect.SelectedShopHidden -> map.dispatch(MapIntent.OnSelectedShopHidden)
                            is ShopOverviewSideEffect.LoginCompleted -> Unit
                        }
                    }
                },
                scope.launch {
                    reviewVm.sideEffect.collect { effect ->
                        effects.send(
                            when (effect) {
                                ShopReviewSideEffect.ShowLoginGuide -> ShopInteractionEffect.ShowLoginGuide
                                is ShopReviewSideEffect.ShowToast ->
                                    ShopInteractionEffect.ShowToast(
                                        com.peto.ramap.designsystem.toast.model
                                            .ToastData(effect.message, effect.type),
                                    )
                            },
                        )
                    }
                },
            )
        if (shopId != null) {
            val selected = map.uiState.value.selectedShop
            overviewVm.dispatch(if (selected == null) ShopOverviewIntent.OnShopIdSelected(shopId) else ShopOverviewIntent.OnShopSelected(selected))
            menuVm.dispatch(ShopMenuIntent.OnShopSelected(shopId))
            reviewVm.dispatch(ShopReviewIntent.OnShopSelected(shopId))
        }
    }

    private fun close() {
        collectors.forEach(Job::cancel)
        collectors = emptyList()
        store?.clear()
        store = null
        overview = null
        menu = null
        review = null
        overviewQuery.clearCache()
        uiState.value = ShopSectionsSnapshot(map.uiState.value)
    }

    fun dispatch(intent: MapIntent) {
        if (intent == MapIntent.OnBookmarkedShopsToggled && !map.uiState.value.isLoggedIn) mapLogin = true
        when {
            intent is MapIntent.OnLoginTypeSelected && !mapLogin -> overview?.dispatch(ShopOverviewIntent.OnLoginTypeSelected(intent.type))
            intent == MapIntent.OnLoginSelectionDismissed && !mapLogin -> overview?.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed)
            else -> map.dispatch(intent)
        }
    }

    fun dispatch(intent: ShopOverviewIntent) {
        if (overview == null) open(null)
        when {
            mapLogin && intent is ShopOverviewIntent.OnLoginTypeSelected -> map.dispatch(MapIntent.OnLoginTypeSelected(intent.type))
            mapLogin && intent == ShopOverviewIntent.OnLoginSelectionDismissed -> {
                map.dispatch(MapIntent.OnLoginSelectionDismissed)
                mapLogin = false
            }
            else -> overview?.dispatch(intent)
        }
    }

    fun dispatch(intent: ShopMenuIntent) {
        menu?.dispatch(intent)
    }

    fun dispatch(intent: ShopReviewIntent) {
        review?.dispatch(intent)
    }
}
