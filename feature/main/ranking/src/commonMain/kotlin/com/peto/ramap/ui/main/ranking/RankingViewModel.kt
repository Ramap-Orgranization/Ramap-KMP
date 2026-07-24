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

            is RankingIntent.OnAreaFilterSelected -> {
                selectAreaFilter(intent.areaFilter)
            }

            is RankingIntent.OnBookmarkChanged -> {
                updateBookmark(intent)
            }

            is RankingIntent.OnCategoryToggled -> {
                toggleCategory(intent)
            }

            is RankingIntent.OnShopClicked -> {
                logShopSelected(intent.shopId)
            }

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
        val enabled =
            intent.category !in currentState.selectedCategories

        val updatedCategories =
            if (enabled) {
                currentState.selectedCategories + intent.category
            } else {
                currentState.selectedCategories - intent.category
            }

        rankingAnalytics.logCategoryToggled(
            categoryId = intent.category.id,
            enabled = enabled,
        )

        changeFilters {
            copy(
                selectedCategories = updatedCategories,
            )
        }
    }

    private fun selectAllCategories() {
        if (currentState.selectedCategories.isEmpty()) {
            return
        }

        rankingAnalytics.logAllCategoriesSelected()

        changeFilters {
            copy(
                selectedCategories = emptySet(),
            )
        }
    }

    private fun selectAreaFilter(areaFilter: AreaFilter) {
        if (currentState.areaFilter == areaFilter) {
            return
        }

        rankingAnalytics.logAreaSelected(areaFilter)

        changeFilters {
            copy(
                areaFilter = areaFilter,
            )
        }
    }

    private fun logShopSelected(shopId: String) {
        val shop =
            currentState.shops
                .firstOrNull { rankingItem ->
                    rankingItem.ranking.shop.id == shopId
                }?.ranking
                ?.shop

        rankingAnalytics.logShopSelected(
            shopId = shopId,
            shopName = shop?.name.orEmpty(),
            hasCategory = shop?.hasCategory ?: false,
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
        if (!loginRepository.hasSession()) {
            rankingAnalytics.logLoginGuideShown()
            postSideEffect(RankingSideEffect.ShowLoginGuide)
            return
        }

        val isCurrentlyBookmarked =
            intent.shopId in currentState.bookmarkedShopIds

        if (isCurrentlyBookmarked == intent.enabled) {
            return
        }

        rankingAnalytics.logBookmarkToggled(
            shopId = intent.shopId,
            enabled = intent.enabled,
        )

        val likeCountDelta =
            if (intent.enabled) {
                1L
            } else {
                -1L
            }

        launchResultTask(
            taskKey = bookmarkTaskKey(intent.shopId),
            policy = TaskPolicy.IgnoreNew,
            onStart = {
                val currentDelta =
                    bookmarkLikeCountDeltas[intent.shopId] ?: 0L

                copy(
                    bookmarkUpdatingShopIds =
                        bookmarkUpdatingShopIds + intent.shopId,
                    bookmarkLikeCountDeltas =
                        bookmarkLikeCountDeltas +
                            (
                                intent.shopId to
                                    currentDelta + likeCountDelta
                            ),
                )
            },
            onFinish = {
                copy(
                    bookmarkUpdatingShopIds =
                        bookmarkUpdatingShopIds - intent.shopId,
                )
            },
            request = {
                personalizationStore.updateBookmark(
                    shopId = intent.shopId,
                    enabled = intent.enabled,
                )
            },
            onError = {
                revertBookmarkLikeCountDelta(
                    shopId = intent.shopId,
                    appliedDelta = likeCountDelta,
                )

                showToast(
                    message =
                        Res.string.personalization_update_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun revertBookmarkLikeCountDelta(
        shopId: String,
        appliedDelta: Long,
    ) {
        reduce {
            val revertedDelta =
                (bookmarkLikeCountDeltas[shopId] ?: 0L) - appliedDelta

            val updatedDeltas =
                if (revertedDelta == 0L) {
                    bookmarkLikeCountDeltas - shopId
                } else {
                    bookmarkLikeCountDeltas +
                        (shopId to revertedDelta)
                }

            copy(
                bookmarkLikeCountDeltas = updatedDeltas,
            )
        }
    }

    private fun loadFirstPage() {
        cancelTask(NEXT_PAGE_TASK_KEY)

        val query =
            currentState.toQuery(
                cursor = null,
            )

        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.FirstPage,
            onStart = {
                copy(
                    showError = false,
                )
            },
            request = {
                shopRankRepository.fetchShopRankings(query)
            },
            onSuccess = ::replaceFirstPage,
            onError = {
                reduce {
                    copy(
                        showError = true,
                    )
                }
            },
        )
    }

    private fun refreshRankings() {
        cancelTask(NEXT_PAGE_TASK_KEY)

        val query =
            currentState.toQuery(
                cursor = null,
            )

        launchResultTask(
            taskKey = RANKINGS_TASK_KEY,
            loadKey = RankingLoadKey.Refresh,
            request = {
                shopRankRepository.fetchShopRankings(query)
            },
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

        if (currentState.isRefreshing || currentState.isLoading) {
            return
        }

        rankingAnalytics.logNextPageRequested()

        val query =
            currentState.toQuery(
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
            onSuccess = ::appendNextPage,
            onError = {
                reduce {
                    copy(
                        showNextPageError = true,
                    )
                }
            },
        )
    }

    private fun replaceFirstPage(page: RankingPage) {
        reduce {
            copy(
                shops = RankedShops(page.items),
                nextCursor = page.nextCursor,
                bookmarkLikeCountDeltas =
                    bookmarkLikeCountDeltas.filterKeys(
                        bookmarkUpdatingShopIds::contains,
                    ),
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
            request = loginRepository::signInWithKakao,
            onSuccess = {
                rankingAnalytics.logLoginSucceeded()
            },
            onError = {
                rankingAnalytics.logLoginFailed()

                showToast(
                    message = Res.string.kakao_login_failure_message,
                    type = ToastType.ERROR,
                )
            },
        )
    }

    private fun RankingUiState.toQuery(cursor: RankingCursor?): RankingQuery =
        RankingQuery(
            area = (areaFilter as? AreaFilter.Selected)?.area,
            categories = selectedCategories,
            cursor = cursor,
        )

    private fun bookmarkTaskKey(shopId: String): String = "$BOOKMARK_TASK_KEY$shopId"

    private fun showToast(
        message: StringResource,
        type: ToastType,
    ) {
        viewModelScope.launch {
            postSideEffect(
                RankingSideEffect.ShowToast(
                    ToastData(
                        message = message,
                        type = type,
                    ),
                ),
            )
        }
    }

    companion object {
        private const val BOOKMARK_TASK_KEY = "bookmark_shop_task:"
        private const val RANKINGS_TASK_KEY = "rankings"
        private const val NEXT_PAGE_TASK_KEY = "ranking-next-page"
        private const val SIGN_IN_TASK_KEY = "ranking-sign-in"
    }
}
