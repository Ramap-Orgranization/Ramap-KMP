package com.peto.ramap.ui.main.ranking

import androidx.lifecycle.viewModelScope
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.analytics.common.login.LoginAnalytics
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
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
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingLoadKey
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
import com.peto.ramap.ui.main.ranking.log.RankingAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.kakao_login_failure_message
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
    private val expectedBookmarkStates = mutableMapOf<String, Boolean>()

    init {
        observePersonalization()
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

            RankingIntent.OnKakaoLoginClicked -> signInWithKakao()

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
            personalizationStore.state.collectLatest { personalization ->
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
        val expectedState = expectedBookmarkStates.remove(shopId)
        if (expectedState == enabled) return
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
        if (!hasBookmarkSessionOrShowGuide()) return
        if (!shouldUpdateBookmark(shop.id, enabled)) return

        rankingAnalytics.logBookmarkToggled(shop, enabled)

        expectedBookmarkStates[shop.id] = enabled
        executeBookmarkUpdate(shop.id, enabled)
    }

    private suspend fun hasBookmarkSessionOrShowGuide(): Boolean {
        if (loginRepository.hasSession()) return true

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
        val likeCountDelta = calculateBookmarkLikeCountDelta(enabled)

        launchResultTask(
            taskKey = bookmarkTaskKey(shopId),
            policy = TaskPolicy.IgnoreNew,
            onStart = { createBookmarkUpdatingState(this, shopId, likeCountDelta) },
            onFinish = { createBookmarkUpdateFinishedState(this, shopId) },
            request = { personalizationStore.updateBookmark(shopId, enabled) },
            onError = {
                expectedBookmarkStates.remove(shopId)
                handleBookmarkUpdateFailure(shopId, likeCountDelta)
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
        likeCountDelta: Long,
    ): RankingUiState {
        val currentDelta =
            state.bookmarkLikeCountDeltas[shopId] ?: 0L

        val updatedDelta =
            currentDelta + likeCountDelta

        return state.copy(
            bookmarkUpdatingShopIds =
                state.bookmarkUpdatingShopIds + shopId,
            bookmarkLikeCountDeltas =
                state.bookmarkLikeCountDeltas +
                    (shopId to updatedDelta),
        )
    }

    private fun createBookmarkUpdateFinishedState(
        state: RankingUiState,
        shopId: String,
    ): RankingUiState =
        state.copy(
            bookmarkUpdatingShopIds =
                state.bookmarkUpdatingShopIds - shopId,
        )

    private fun handleBookmarkUpdateFailure(
        shopId: String,
        appliedDelta: Long,
    ) {
        revertBookmarkLikeCountDelta(
            shopId = shopId,
            appliedDelta = appliedDelta,
        )

        showToast(
            message = Res.string.personalization_update_failure_message,
            type = ToastType.ERROR,
        )
    }

    private fun revertBookmarkLikeCountDelta(
        shopId: String,
        appliedDelta: Long,
    ) {
        reduce {
            copy(
                bookmarkLikeCountDeltas =
                    createRevertedBookmarkLikeCountDeltas(
                        currentDeltas = bookmarkLikeCountDeltas,
                        shopId = shopId,
                        appliedDelta = appliedDelta,
                    ),
            )
        }
    }

    private fun createRevertedBookmarkLikeCountDeltas(
        currentDeltas: Map<String, Long>,
        shopId: String,
        appliedDelta: Long,
    ): Map<String, Long> {
        val currentDelta = currentDeltas[shopId] ?: 0L

        val revertedDelta = currentDelta - appliedDelta

        return if (revertedDelta == 0L) {
            currentDeltas - shopId
        } else {
            currentDeltas + (shopId to revertedDelta)
        }
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

    private fun signInWithKakao() {
        loginAnalytics.logLoginStarted(AnalyticsSource.RANKING)

        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::signInWithKakao,
            onSuccess = { loginAnalytics.logLoginSucceeded(AnalyticsSource.RANKING) },
            onError = { handleKakaoLoginFailure() },
        )
    }

    private fun handleKakaoLoginFailure() {
        loginAnalytics.logLoginFailed(AnalyticsSource.RANKING)

        showToast(Res.string.kakao_login_failure_message, ToastType.ERROR)
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
