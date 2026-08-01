package com.peto.ramap.ui.main.ranking

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.analytics.common.login.LoginMethod
import com.peto.ramap.designsystem.component.LoginTypeResourceMapper
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingLoadKey
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
import com.peto.ramap.ui.main.ranking.log.RankingAnalytics
import com.peto.ramap.ui.main.ranking.model.PendingRankingAction
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.personalization_update_failure_message
import ramap.shared.generated.resources.ranking_refresh_failure_message

class RankingViewModel(
    private val shopRankRepository: ShopRankingRepository,
    private val personalizationStore: ShopPersonalizationStore,
    private val loginRepository: LoginRepository,
    private val rankingAnalytics: RankingAnalytics,
    private val loginAnalytics: LoginAnalytics,
) : BaseViewModel<RankingUiState, RankingIntent, RankingSideEffect>(
        RankingUiState(),
    ) {
    private var observedBookmarkedShopIds: Set<String>? = null
    private var pendingRankingAction: PendingRankingAction? = null
    private var pendingKakaoLogin = false

    init {
        observePersonalization()
        viewModelScope.launch { observeSessionState() }
        loadFirstPage()
    }

    override suspend fun handleIntent(intent: RankingIntent) {
        when (intent) {
            RankingIntent.OnRefreshed -> refreshRankings()

            RankingIntent.OnRetried -> loadFirstPage()

            RankingIntent.OnNextPageRequested,
            RankingIntent.OnNextPageRetried,
            -> loadNextPage()

            RankingIntent.OnAllCategoriesSelected -> selectAllCategories()

            RankingIntent.OnAreaSheetOpened -> restoreAreaSelection()

            is RankingIntent.OnLoginTypeSelected -> signIn(intent.type)

            RankingIntent.OnLoginSelectionDismissed -> clearPendingAction()

            is RankingIntent.OnAreaFilterSelected -> selectAreaFilter(intent.areaFilter)

            is RankingIntent.OnAdministrativeAreaSelected -> selectAdministrativeArea(intent.area)

            RankingIntent.OnAreaSelectionBack -> showAdministrativeAreas()

            is RankingIntent.OnBookmarkChanged -> updateBookmark(intent.shop, intent.enabled)

            is RankingIntent.OnCategoryToggled -> toggleCategory(intent)

            is RankingIntent.OnShopClicked -> rankingAnalytics.logShopSelected(intent.shop)
        }
    }

    private fun observePersonalization() {
        viewModelScope.launch {
            personalizationStore.state.collectLatest { state ->
                val personalization =
                    (state as? PersonalizationBootstrapState.Success)?.value
                        ?: return@collectLatest
                synchronizeLikeCounts(personalization.bookmarkedShopIds)
                reduce {
                    copy(
                        bookmarkedShopIds = personalization.bookmarkedShopIds,
                    )
                }
            }
        }
    }

    private fun synchronizeLikeCounts(bookmarkedShopIds: Set<String>) {
        val previousIds = observedBookmarkedShopIds
        observedBookmarkedShopIds = bookmarkedShopIds
        if (previousIds == null) return

        val addedShopIds = bookmarkedShopIds - previousIds
        val removedShopIds = previousIds - bookmarkedShopIds
        val loadedShopIds = currentState.shops.map { it.ranking.shop.id }.toSet()
        val changedDeltas = mutableMapOf<String, Long>()

        for (shopId in addedShopIds) {
            synchronizeLikeCountChange(shopId, enabled = true, loadedShopIds, changedDeltas)
        }
        for (shopId in removedShopIds) {
            synchronizeLikeCountChange(shopId, enabled = false, loadedShopIds, changedDeltas)
        }
        if (changedDeltas.isEmpty()) return

        reduce {
            copy(
                bookmarkLikeCountDeltas =
                    bookmarkLikeCountDeltas.toMutableMap().apply {
                        for ((shopId, delta) in changedDeltas) {
                            val updatedDelta = (this[shopId] ?: 0L) + delta
                            if (updatedDelta == 0L) remove(shopId) else this[shopId] = updatedDelta
                        }
                    },
            )
        }
    }

    private fun synchronizeLikeCountChange(
        shopId: String,
        enabled: Boolean,
        loadedShopIds: Set<String>,
        changedDeltas: MutableMap<String, Long>,
    ) {
        if (shopId !in loadedShopIds) return

        changedDeltas[shopId] = calculateBookmarkLikeCountDelta(enabled)
    }

    private fun toggleCategory(intent: RankingIntent.OnCategoryToggled) {
        val enabled =
            intent.category !in currentState.selectedCategories

        val updatedCategories =
            if (enabled) {
                currentState.selectedCategories + intent.category
            } else {
                currentState.selectedCategories - intent.category
            }

        rankingAnalytics.logCategoryToggled(intent.category.id, enabled)
        changeFilters { copy(selectedCategories = updatedCategories) }
    }

    private fun selectAllCategories() {
        if (currentState.selectedCategories.isEmpty()) return

        changeFilters { copy(selectedCategories = emptySet()) }
    }

    private fun selectAreaFilter(areaFilter: AreaFilter) {
        if (currentState.areaFilter == areaFilter) return

        rankingAnalytics.logAreaSelected(areaFilter)

        changeFilters {
            copy(
                areaFilter = areaFilter,
            )
        }
    }

    private fun selectAdministrativeArea(area: AdministrativeArea) {
        if (area == AdministrativeArea.SEJONG) {
            selectAreaFilter(AreaFilter.Province(area))
            return
        }

        loadAdministrativeDistricts(area)
    }

    private fun restoreAreaSelection() {
        when (val areaFilter = currentState.areaFilter) {
            AreaFilter.Nationwide -> showAdministrativeAreas()
            is AreaFilter.Province -> restoreAdministrativeArea(areaFilter.area)
            is AreaFilter.District -> restoreAdministrativeArea(areaFilter.area)
        }
    }

    private fun restoreAdministrativeArea(area: AdministrativeArea) {
        if (area == AdministrativeArea.SEJONG) {
            reduce {
                copy(
                    areaSelectionArea = area,
                    administrativeDistricts = AdministrativeDistricts(emptyList()),
                )
            }
            return
        }

        loadAdministrativeDistricts(area)
    }

    private fun showAdministrativeAreas() {
        cancelTask(DISTRICTS_TASK_KEY)
        reduce {
            copy(
                areaSelectionArea = null,
                administrativeDistricts = AdministrativeDistricts(emptyList()),
            )
        }
    }

    private fun loadAdministrativeDistricts(area: AdministrativeArea) {
        launchResultTask(
            taskKey = DISTRICTS_TASK_KEY,
            loadKey = RankingLoadKey.Districts,
            policy = TaskPolicy.CancelPrevious,
            onStart = {
                copy(
                    areaSelectionArea = area,
                    administrativeDistricts = AdministrativeDistricts(emptyList()),
                )
            },
            request = { shopRankRepository.fetchAdministrativeDistricts(area) },
            onSuccess = { districts ->
                reduce { copy(administrativeDistricts = districts) }
            },
        )
    }

    private fun changeFilters(reducer: RankingUiState.() -> RankingUiState) {
        reduce {
            reducer().copy(
                shops = RankedShops(ShopRankings(emptyList())),
                nextCursor = null,
                showError = false,
                showNextPageError = false,
            )
        }

        loadFirstPage()
    }

    private suspend fun updateBookmark(
        shop: RamenShop,
        enabled: Boolean,
    ) {
        val action = PendingRankingAction(shop, enabled)
        if (!hasBookmarkSessionOrShowGuide(action)) return
        executeBookmarkUpdate(action)
    }

    private suspend fun executeBookmarkUpdate(action: PendingRankingAction) {
        val shop = action.shop
        val enabled = action.enabled
        if (shop.id in currentState.bookmarkUpdatingShopIds) return
        if (!shouldUpdateBookmark(shop.id, enabled)) return

        rankingAnalytics.logBookmarkToggled(shop, enabled)

        executeBookmarkUpdate(shop.id, enabled)
    }

    private suspend fun hasBookmarkSessionOrShowGuide(action: PendingRankingAction): Boolean {
        if (loginRepository.hasSession()) return true

        pendingRankingAction = action
        postSideEffect(RankingSideEffect.ShowLoginGuide)
        return false
    }

    private fun shouldUpdateBookmark(
        shopId: String,
        enabled: Boolean,
    ): Boolean {
        val isCurrentlyBookmarked = shopId in currentState.bookmarkedShopIds

        return isCurrentlyBookmarked != enabled
    }

    private fun executeBookmarkUpdate(
        shopId: String,
        enabled: Boolean,
    ) {
        launchResultTask(
            taskKey = bookmarkTaskKey(shopId),
            policy = TaskPolicy.IgnoreNew,
            onStart = { createBookmarkUpdatingState(this, shopId) },
            onFinish = { createBookmarkUpdateFinishedState(this, shopId) },
            request = { personalizationStore.updateBookmark(shopId, enabled) },
            onError = {
                handleBookmarkUpdateFailure()
            },
        )
    }

    private fun calculateBookmarkLikeCountDelta(enabled: Boolean): Long =
        if (enabled) {
            1L
        } else {
            -1L
        }

    private fun createBookmarkUpdatingState(
        state: RankingUiState,
        shopId: String,
    ): RankingUiState =
        state.copy(
            bookmarkUpdatingShopIds =
                state.bookmarkUpdatingShopIds + shopId,
        )

    private fun createBookmarkUpdateFinishedState(
        state: RankingUiState,
        shopId: String,
    ): RankingUiState =
        state.copy(
            bookmarkUpdatingShopIds =
                state.bookmarkUpdatingShopIds - shopId,
        )

    private fun handleBookmarkUpdateFailure() {
        showToast(
            message = Res.string.personalization_update_failure_message,
            type = ToastType.ERROR,
        )
    }

    private fun loadFirstPage() {
        cancelTask(NEXT_PAGE_TASK_KEY)

        val query = createRankingQuery(cursor = null)

        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.FirstPage,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(showError = false) },
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = ::replaceFirstPage,
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshRankings() {
        cancelTask(NEXT_PAGE_TASK_KEY)

        val query = createRankingQuery(cursor = null)

        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.Refresh,
            policy = TaskPolicy.CancelPrevious,
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = ::replaceFirstPage,
            onError = { showRefreshFailure() },
        )
    }

    private fun showRefreshFailure() {
        showToast(
            message = Res.string.ranking_refresh_failure_message,
            type = ToastType.ERROR,
        )
    }

    private fun loadNextPage() {
        val cursor = currentState.nextCursor ?: return

        if (!canLoadNextPage()) return

        rankingAnalytics.logNextPageRequested()

        val query = createRankingQuery(cursor = cursor)

        launchResultTask(
            taskKey = NEXT_PAGE_TASK_KEY,
            loadKey = RankingLoadKey.NextPage,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showNextPageError = false) },
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = { page -> appendNextPage(page) },
            onError = { showNextPageLoadFailure() },
        )
    }

    private fun canLoadNextPage(): Boolean =
        !currentState.isRefreshing &&
            !currentState.isLoading &&
            !currentState.isLoadingNext

    private fun showNextPageLoadFailure() {
        reduce { copy(showNextPageError = true) }
    }

    private fun replaceFirstPage(page: RankingPage) {
        reduce {
            val remainingLikeCountDeltas =
                bookmarkLikeCountDeltas.filterKeys { shopId ->
                    shopId in bookmarkUpdatingShopIds
                }

            copy(
                shops = RankedShops(page.items),
                nextCursor = page.nextCursor,
                bookmarkLikeCountDeltas = remainingLikeCountDeltas,
                showError = false,
                showNextPageError = false,
            )
        }
    }

    private fun appendNextPage(page: RankingPage) {
        reduce {
            copy(
                shops = shops.appendNextPage(page.items),
                nextCursor = page.nextCursor,
                showNextPageError = false,
            )
        }
    }

    private fun signIn(type: LoginType) {
        val method = loginMethod(type)
        loginAnalytics.logLoginStarted(AnalyticsSource.RANKING, method)
        if (type == LoginType.KAKAO) pendingKakaoLogin = true

        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signIn(type) },
            onSuccess = {
                if (type == LoginType.APPLE) completeLogin(type)
            },
            onError = {
                if (type == LoginType.KAKAO) pendingKakaoLogin = false
                loginAnalytics.logLoginFailed(AnalyticsSource.RANKING, method)
                showLoginFailure(type)
                clearPendingAction()
            },
        )
    }

    private suspend fun observeSessionState() {
        loginRepository.sessionState.collectLatest { sessionState ->
            completeKakaoLoginIfAuthenticated(sessionState == LoginSessionState.AUTHENTICATED)
        }
    }

    private fun completeKakaoLoginIfAuthenticated(isAuthenticated: Boolean) {
        if (!isAuthenticated || !pendingKakaoLogin) return

        pendingKakaoLogin = false
        completeLogin(LoginType.KAKAO)
    }

    private fun completeLogin(type: LoginType) {
        loginAnalytics.logLoginSucceeded(AnalyticsSource.RANKING, loginMethod(type))
        resumePendingAction()
    }

    private fun resumePendingAction() {
        val action = pendingRankingAction ?: return
        pendingRankingAction = null
        viewModelScope.launch { executeBookmarkUpdate(action) }
    }

    private fun clearPendingAction() {
        pendingRankingAction = null
    }

    private fun showLoginFailure(type: LoginType) {
        showToast(LoginTypeResourceMapper.failureMessage(type), ToastType.ERROR)
    }

    private fun loginMethod(type: LoginType): LoginMethod =
        when (type) {
            LoginType.KAKAO -> LoginMethod.KAKAO
            LoginType.APPLE -> LoginMethod.APPLE
        }

    private fun createRankingQuery(cursor: RankingCursor?): RankingQuery =
        RankingQuery(
            areaFilter = currentState.areaFilter,
            categories = currentState.selectedCategories,
            cursor = cursor,
        )

    private fun bookmarkTaskKey(shopId: String): String = "$BOOKMARK_TASK_KEY$shopId"

    private fun showToast(
        message: StringResource,
        type: ToastType,
    ) {
        viewModelScope.launch {
            postSideEffect(RankingSideEffect.ShowToast(ToastData(message, type)))
        }
    }

    companion object {
        private const val BOOKMARK_TASK_KEY = "bookmark-shop:"
        private const val RANKINGS_TASK_KEY = "rankings"
        private const val NEXT_PAGE_TASK_KEY = "ranking-next-page"
        private const val SIGN_IN_TASK_KEY = "ranking-sign-in"
        private const val DISTRICTS_TASK_KEY = "ranking-districts"
    }
}
