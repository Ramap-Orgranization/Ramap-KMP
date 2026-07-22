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
) : BaseViewModel<RankingUiState, RankingIntent, RankingSideEffect>(RankingUiState()) {
    init {
        observePersonalization()
        loadFirstPage()
    }

    override suspend fun handleIntent(intent: RankingIntent) {
        when (intent) {
            RankingIntent.OnRefreshed -> refreshRankings()
            RankingIntent.OnNextPageRequested, RankingIntent.OnNextPageRetried -> loadNextPage()
            RankingIntent.OnAllCategoriesSelected -> selectAllCategories()
            RankingIntent.OnKakaoLoginClicked -> signInWithKakao()
            is RankingIntent.OnAreaFilterSelected -> selectAreaFilter(intent.areaFilter)
            is RankingIntent.OnBookmarkChanged -> updateBookmark(intent)
            is RankingIntent.OnCategoryToggled -> toggleCategory(intent)
            else -> Unit
        }
    }

    private fun observePersonalization() {
        viewModelScope.launch {
            personalizationStore.state.collectLatest { personalization ->
                reduce { copy(bookmarkedShopIds = personalization.bookmarkedShopIds) }
            }
        }
    }

    private fun toggleCategory(intent: RankingIntent.OnCategoryToggled) {
        val categories =
            if (intent.category in currentState.selectedCategories) {
                currentState.selectedCategories - intent.category
            } else {
                currentState.selectedCategories + intent.category
            }
        changeFilters { copy(selectedCategories = categories) }
    }

    private fun selectAllCategories() {
        if (currentState.selectedCategories.isEmpty()) return
        changeFilters { copy(selectedCategories = emptySet()) }
    }

    private fun selectAreaFilter(areaFilter: AreaFilter) {
        if (currentState.areaFilter == areaFilter) return
        changeFilters { copy(areaFilter = areaFilter) }
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
        if (!loginRepository.hasSession()) {
            postSideEffect(RankingSideEffect.ShowLoginGuide)
            return
        }
        if ((intent.shopId in currentState.bookmarkedShopIds) == intent.enabled) return

        val likeCount = if (intent.enabled) 1L else -1L
        launchResultTask(
            taskKey = bookmarkTaskKey(intent.shopId),
            policy = TaskPolicy.IgnoreNew,
            onStart = {
                val currentDelta = bookmarkLikeCountDeltas[intent.shopId] ?: 0L
                copy(
                    bookmarkUpdatingShopIds = bookmarkUpdatingShopIds + intent.shopId,
                    bookmarkLikeCountDeltas =
                        bookmarkLikeCountDeltas +
                            (intent.shopId to currentDelta + likeCount),
                )
            },
            onFinish = { copy(bookmarkUpdatingShopIds = bookmarkUpdatingShopIds - intent.shopId) },
            request = { personalizationStore.updateBookmark(intent.shopId, intent.enabled) },
            onError = {
                reduce {
                    val revertedDelta =
                        (bookmarkLikeCountDeltas[intent.shopId] ?: 0L) - likeCount
                    copy(
                        bookmarkLikeCountDeltas =
                            if (revertedDelta == 0L) {
                                bookmarkLikeCountDeltas - intent.shopId
                            } else {
                                bookmarkLikeCountDeltas + (intent.shopId to revertedDelta)
                            },
                    )
                }
                showToast(Res.string.personalization_update_failure_message, ToastType.ERROR)
            },
        )
    }

    private fun loadFirstPage() {
        cancelTask(NEXT_PAGE_TASK_KEY)
        val query = currentState.toQuery(cursor = null)
        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.FirstPage,
            onStart = { copy(showError = false) },
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = ::replaceFirstPage,
            onError = { reduce { copy(showError = true) } },
        )
    }

    private fun refreshRankings() {
        cancelTask(NEXT_PAGE_TASK_KEY)
        val query = currentState.toQuery(cursor = null)
        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.Refresh,
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = ::replaceFirstPage,
            onError = {
                showToast(
                    message = Res.string.ranking_refresh_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun loadNextPage() {
        val cursor = currentState.nextCursor ?: return
        if (currentState.isRefreshing || currentState.isLoading) return
        val query = currentState.toQuery(cursor)
        launchResultTask(
            taskKey = NEXT_PAGE_TASK_KEY,
            loadKey = RankingLoadKey.NextPage,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(showNextPageError = false) },
            request = { shopRankRepository.fetchShopRankings(query) },
            onSuccess = ::appendNextPage,
            onError = { reduce { copy(showNextPageError = true) } },
        )
    }

    private fun replaceFirstPage(page: RankingPage) {
        reduce {
            copy(
                shops = RankedShops(page.items),
                nextCursor = page.nextCursor,
                bookmarkLikeCountDeltas =
                    bookmarkLikeCountDeltas.filterKeys(bookmarkUpdatingShopIds::contains),
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

    private fun RankingUiState.toQuery(cursor: RankingCursor?): RankingQuery =
        RankingQuery(
            area = (areaFilter as? AreaFilter.Selected)?.area,
            categories = selectedCategories,
            cursor = cursor,
        )

    private fun signInWithKakao() {
        launchResultTask(
            taskKey = SIGN_IN_TASK_KEY,
            policy = TaskPolicy.IgnoreNew,
            request = loginRepository::signInWithKakao,
            onError = { showToast(Res.string.kakao_login_failure_message, ToastType.ERROR) },
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
