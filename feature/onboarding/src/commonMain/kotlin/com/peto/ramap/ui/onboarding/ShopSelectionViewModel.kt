package com.peto.ramap.ui.onboarding

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.domain.model.rank.RankingQuery
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopRankingRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.contract.ShopSelectionSideEffect
import com.peto.ramap.ui.onboarding.contract.ShopSelectionUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_completion_failure
import ramap.shared.generated.resources.personalization_update_failure_message

internal class ShopSelectionViewModel(
    private val searchShops: SearchRamenShopsUseCase,
    private val shopRepository: RamenShopRepository,
    private val rankingRepository: ShopRankingRepository,
    private val personalizationStore: PersonalizationStore,
    private val loginRepository: LoginRepository,
    private val storage: OnboardingStorage,
) : BaseViewModel<ShopSelectionUiState, ShopSelectionIntent, ShopSelectionSideEffect>(ShopSelectionUiState()) {
    private var bookmarkOwnerAccountId: String? = null

    init {
        loadPersonalization()
    }

    override suspend fun handleIntent(intent: ShopSelectionIntent) {
        when (intent) {
            is ShopSelectionIntent.QueryChanged -> changeQuery(intent.query)
            is ShopSelectionIntent.ToggleBookmark -> toggleBookmark(intent.shopId)
            ShopSelectionIntent.Search -> search()
            ShopSelectionIntent.RetryShops -> if (currentState.isSearching) search() else loadSuggestions()
            ShopSelectionIntent.RetryPersonalization -> loadPersonalization()
            ShopSelectionIntent.Complete -> complete()
        }
    }

    private fun loadPersonalization() {
        cancelTask(OBSERVE_PERSONALIZATION_TASK)
        launchResultTask(
            taskKey = PERSONALIZATION_TASK,
            loadKey = ShopSelectionLoadKey.Personalization,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(hasPersonalization = false, hasPersonalizationError = false) },
            request = {
                try {
                    loginRepository.awaitInitialization()
                    bookmarkOwnerAccountId = loginRepository.currentAccountId()
                    personalizationStore.refresh()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    RamapResult.Error(RamapError.Unknown(exception))
                }
            },
            onSuccess = {
                observePersonalization()
                loadSuggestions()
            },
            onError = { reduce { copy(hasPersonalizationError = true) } },
        )
    }

    private fun observePersonalization() {
        launchTask(taskKey = OBSERVE_PERSONALIZATION_TASK) {
            personalizationStore.state.collect { state ->
                val personalization = (state as? PersonalizationBootstrapState.Success)?.value
                reduce {
                    copy(
                        bookmarks = OnboardingBookmarks(personalization?.bookmarkedShopIds.orEmpty()),
                        hasPersonalization = personalization != null,
                        hasPersonalizationError = state is PersonalizationBootstrapState.Error,
                    )
                }
            }
        }
    }

    private fun loadSuggestions() {
        launchResultTask(
            taskKey = SUGGESTIONS_TASK,
            loadKey = ShopSelectionLoadKey.Suggestions,
            onStart = { copy(hasSuggestionsError = false) },
            request = { fetchSuggestions() },
            onSuccess = { shops -> reduce { copy(suggestedShops = RamenShops(suggestedShops + shops)) } },
            onError = { reduce { copy(hasSuggestionsError = true) } },
        )
    }

    private suspend fun fetchSuggestions(): RamapResult<RamenShops> {
        val savedIds = (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value?.bookmarkedShopIds.orEmpty()
        val savedShops = if (savedIds.isEmpty()) RamapResult.Success(RamenShops(emptyMap())) else shopRepository.fetchRamenShops(savedIds)
        if (savedShops is RamapResult.Error) return savedShops
        val rankings = rankingRepository.fetchShopRankings(RankingQuery(AreaFilter.Nationwide, emptySet(), limit = SUGGESTION_LIMIT))
        if (rankings is RamapResult.Error) return rankings
        return RamapResult.Success(
            RamenShops((savedShops as RamapResult.Success).data.values + (rankings as RamapResult.Success).data.items.map { it.shop }),
        )
    }

    private fun changeQuery(query: String) {
        cancelTask(SEARCH_TASK)
        reduce { copy(query = query.take(SearchQuery.MAX_LENGTH), searchResults = RamenShops(emptyMap()), hasSearchError = false) }
        search(debounce = true)
    }

    private fun search(debounce: Boolean = false) {
        val query = SearchQuery(currentState.query).normalizeShopSearchQuery()
        if (!query.isValidForShopSearch()) return
        launchResultTask(
            taskKey = SEARCH_TASK,
            loadKey = ShopSelectionLoadKey.Search,
            onStart = { copy(hasSearchError = false) },
            request = {
                if (debounce) delay(SEARCH_DEBOUNCE_MS)
                searchShops(query)
            },
            onSuccess = { shops -> reduce { copy(searchResults = shops) } },
            onError = { reduce { copy(hasSearchError = true) } },
        )
    }

    private fun toggleBookmark(shopId: String) {
        if (!currentState.canChangeBookmarks || !isCurrentBookmarkOwner()) return
        val shop = currentState.shops[shopId] ?: return
        val enabled = shopId !in currentState.bookmarks
        launchResultTask(
            taskKey = BOOKMARK_TASK,
            loadKey = ShopSelectionLoadKey.BookmarkUpdates,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actionError = null) },
            request = { personalizationStore.updateBookmark(shopId, enabled) },
            onSuccess = {
                reduce { copy(suggestedShops = RamenShops(suggestedShops + (shopId to shop))) }
            },
            onError = { reduce { copy(actionError = Res.string.personalization_update_failure_message) } },
        )
    }

    private fun isCurrentBookmarkOwner(): Boolean {
        if (bookmarkOwnerAccountId == loginRepository.currentAccountId()) return true
        loadPersonalization()
        return false
    }

    private fun complete() {
        if (!currentState.canComplete || !isCurrentBookmarkOwner()) return
        launchTask(
            taskKey = COMPLETE_TASK,
            loadKey = ShopSelectionLoadKey.Complete,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(actionError = null) },
        ) {
            try {
                storage.completeOnboarding()
                cancelTask(OBSERVE_PERSONALIZATION_TASK)
                cancelTask(SEARCH_TASK)
                cancelTask(SUGGESTIONS_TASK)
                reduce { copy(isCompleted = true) }
                postSideEffect(ShopSelectionSideEffect.Completed)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                handleError(exception)
                reduce { copy(actionError = Res.string.onboarding_completion_failure) }
            }
        }
    }

    private companion object {
        const val PERSONALIZATION_TASK = "onboarding-personalization"
        const val OBSERVE_PERSONALIZATION_TASK = "onboarding-observe-personalization"
        const val SUGGESTIONS_TASK = "onboarding-suggestions"
        const val SEARCH_TASK = "onboarding-search"
        const val BOOKMARK_TASK = "onboarding-bookmark"
        const val COMPLETE_TASK = "onboarding-complete"
        const val SUGGESTION_LIMIT = 20
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
