package com.peto.ramap.ui.main.ranking

import androidx.lifecycle.viewModelScope
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.rank.RankedShops
import com.peto.ramap.domain.model.rank.RankingCursor
import com.peto.ramap.domain.model.rank.RankingPage
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.rank.ShopRankings
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.domain.store.ShopPersonalizationStore
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.main.ranking.contract.RankingIntent
import com.peto.ramap.ui.main.ranking.contract.RankingLoadKey
import com.peto.ramap.ui.main.ranking.contract.RankingSideEffect
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
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
) : BaseViewModel<RankingUiState, RankingIntent, RankingSideEffect>(
        RankingUiState(),
    ) {
    init {
        observePersonalization()
        loadFirstPage()
    }

    override suspend fun handleIntent(intent: RankingIntent) {
        when (intent) {
            RankingIntent.OnRefreshed -> refreshRankings()

            RankingIntent.OnNextPageRequested,
            RankingIntent.OnNextPageRetried,
            -> loadNextPage()

            RankingIntent.OnAllCategoriesSelected -> selectAllCategories()
            RankingIntent.OnKakaoLoginClicked -> signInWithKakao()

            is RankingIntent.OnAreaFilterSelected -> selectAreaFilter(intent.areaFilter)

            is RankingIntent.OnBookmarkChanged -> updateBookmark(intent)

            is RankingIntent.OnCategoryToggled -> toggleCategory(intent)

            is RankingIntent.OnShopClicked -> logShopSelected(intent.shopId)

            else -> Unit
        }
    }

    private fun observePersonalization() {
        viewModelScope.launch {
            personalizationStore.state.collectLatest { personalization ->
                reduce {
                    copy(
                        bookmarkedShopIds = personalization.bookmarkedShopIds,
                    )
                }
            }
        }
    }

    private fun toggleCategory(intent: RankingIntent.OnCategoryToggled) {
        val enabled = intent.category !in currentState.selectedCategories

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

        rankingAnalytics.logAllCategoriesSelected()

        changeFilters { copy(selectedCategories = emptySet()) }
    }

    private fun selectAreaFilter(areaFilter: AreaFilter) {
        if (currentState.areaFilter == areaFilter) return

        rankingAnalytics.logAreaSelected(areaFilter)

        changeFilters { copy(areaFilter = areaFilter) }
    }

    private fun logShopSelected(shopId: String) {
        val selectedShop =
            currentState.shops
                .firstOrNull { rankingItem -> rankingItem.ranking.shop.id == shopId }
                ?.ranking
                ?.shop

        rankingAnalytics.logShopSelected(
            shopId = shopId,
            shopName = selectedShop?.name.orEmpty(),
            hasCategory = selectedShop?.hasCategory ?: false,
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

    private suspend fun updateBookmark(intent: RankingIntent.OnBookmarkChanged) {
        if (!checkBookmarkSession()) return

        val shouldUpdate = shouldUpdateBookmark(intent.shopId, intent.enabled)

        if (!shouldUpdate) return

        rankingAnalytics.logBookmarkToggled(intent.shopId, intent.enabled)
        executeBookmarkUpdate(intent.shopId, intent.enabled)
    }

    private suspend fun checkBookmarkSession(): Boolean {
        if (loginRepository.hasSession()) {
            return true
        }

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

    private fun logBookmarkToggled(
        shopId: String,
        enabled: Boolean,
    ) {
        rankingAnalytics.logBookmarkToggled(
            shopId = shopId,
            enabled = enabled,
        )
    }

    private fun executeBookmarkUpdate(
        shopId: String,
        enabled: Boolean,
    ) {
        val likeCountDelta =
            calculateBookmarkLikeCountDelta(enabled)

        launchResultTask(
            taskKey = bookmarkTaskKey(shopId),
            policy = TaskPolicy.IgnoreNew,
            onStart = {
                createBookmarkUpdatingState(
                    state = this,
                    shopId = shopId,
                    likeCountDelta = likeCountDelta,
                )
            },
            onFinish = {
                createBookmarkUpdateFinishedState(
                    state = this,
                    shopId = shopId,
                )
            },
            request = {
                personalizationStore.updateBookmark(
                    shopId = shopId,
                    enabled = enabled,
                )
            },
            onError = {
                handleBookmarkUpdateFailure(
                    shopId = shopId,
                    appliedDelta = likeCountDelta,
                )
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
        revertBookmarkLikeCountDelta(shopId = shopId, appliedDelta = appliedDelta)
        showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
    }

    private fun revertBookmarkLikeCountDelta(
        shopId: String,
        appliedDelta: Long,
    ) {
        reduce {
            val updatedDeltas =
                createRevertedBookmarkLikeCountDeltas(
                    currentDeltas = bookmarkLikeCountDeltas,
                    shopId = shopId,
                    appliedDelta = appliedDelta,
                )

            copy(
                bookmarkLikeCountDeltas = updatedDeltas,
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
            onStart = { copy(showError = false) },
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = { page -> replaceFirstPage(page) },
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshRankings() {
        cancelTask(NEXT_PAGE_TASK_KEY)

        val query = createRankingQuery(cursor = null)

        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.Refresh,
            request = {
                shopRankRepository.fetchShopRankings(query)
            },
            onSuccess = { page ->
                replaceFirstPage(page)
            },
            onError = {
                handleRefreshFailure()
            },
        )
    }

    private fun handleRefreshFailure() {
        showToast(
            message = Res.string.ranking_refresh_failure_message,
            type = ToastType.ERROR,
        )
    }

    private fun loadNextPage() {
        val cursor =
            currentState.nextCursor ?: return

        if (!canLoadNextPage()) {
            return
        }

        rankingAnalytics.logNextPageRequested()

        val query =
            createRankingQuery(
                cursor = cursor,
            )

        launchResultTask(
            taskKey = NEXT_PAGE_TASK_KEY,
            loadKey = RankingLoadKey.NextPage,
            policy = TaskPolicy.IgnoreNew,
            onStart = {
                copy(
                    showNextPageError = false,
                )
            },
            request = {
                shopRankRepository.fetchShopRankings(query)
            },
            onSuccess = { page ->
                appendNextPage(page)
            },
            onError = {
                handleNextPageLoadFailure()
            },
        )
    }

    private fun canLoadNextPage(): Boolean =
        !currentState.isRefreshing &&
            !currentState.isLoading

    private fun handleNextPageLoadFailure() {
        reduce {
            copy(
                showNextPageError = true,
            )
        }
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
        rankingAnalytics.logLoginStarted()

        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = { loginRepository.signInWithKakao() },
            onSuccess = { rankingAnalytics.logLoginSucceeded() },
            onError = { handleKakaoLoginFailure() },
        )
    }

    private fun handleKakaoLoginFailure() {
        rankingAnalytics.logLoginFailed()
        showToast(message = Res.string.kakao_login_failure_message, type = ToastType.ERROR)
    }

    private fun createRankingQuery(cursor: RankingCursor?): RankingQuery {
        val selectedAreaFilter = currentState.areaFilter as? AreaFilter.Selected

        return RankingQuery(
            area = selectedAreaFilter?.area,
            categories = currentState.selectedCategories,
            cursor = cursor,
        )
    }

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
        private const val BOOKMARK_TASK_KEY = "bookmark_shop_task:"
        private const val RANKINGS_TASK_KEY = "rankings"
        private const val NEXT_PAGE_TASK_KEY = "ranking-next-page"
        private const val SIGN_IN_TASK_KEY = "ranking-sign-in"
    }
}
