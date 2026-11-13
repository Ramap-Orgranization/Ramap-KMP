package com.peto.ramap.ui.onboarding.shopselection

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsPage
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopSuggestionsRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.domain.usecase.SearchRamenShopsUseCase
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionSideEffect
import com.peto.ramap.ui.onboarding.shopselection.contract.ShopSelectionUiState
import com.peto.ramap.ui.onboarding.shopselection.log.ShopSelectionAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_completion_failure
import ramap.shared.generated.resources.personalization_update_failure_message
import kotlin.time.Duration.Companion.milliseconds

public class ShopSelectionViewModel(
    private val searchShops: SearchRamenShopsUseCase,
    private val shopRepository: RamenShopRepository,
    private val suggestionsRepository: ShopSuggestionsRepository,
    private val personalizationStore: PersonalizationStore,
    private val loginRepository: LoginRepository,
    private val storage: OnboardingStorage,
    private val analytics: ShopSelectionAnalytics,
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
            ShopSelectionIntent.LoadNextPage -> loadNextPage()
            ShopSelectionIntent.RetryNextPage -> loadNextPage(isRetry = true)
            ShopSelectionIntent.RetryPersonalization -> loadPersonalization()
            ShopSelectionIntent.Complete -> complete()
        }
    }

    private fun loadPersonalization() {
        cancelTask(OBSERVE_PERSONALIZATION_TASK)
        cancelTask(SUGGESTIONS_TASK)
        cancelTask(NEXT_PAGE_TASK)
        launchResultTask(
            taskKey = PERSONALIZATION_TASK,
            loadKey = ShopSelectionLoadKey.Personalization,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(hasPersonalization = false, hasPersonalizationError = false, nextCursor = null, hasNextPageError = false) },
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
            onError = { error ->
                analytics.shopLoadFailed("personalization", error)
                reduce { copy(hasPersonalizationError = true) }
            },
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
        cancelTask(NEXT_PAGE_TASK)
        launchResultTask(
            taskKey = SUGGESTIONS_TASK,
            loadKey = ShopSelectionLoadKey.Suggestions,
            onStart = { copy(hasSuggestionsError = false, nextCursor = null, hasNextPageError = false) },
            request = { fetchSuggestions() },
            onSuccess = { page ->
                analytics.shopLoadResult("suggestions", page.shops.size)
                reduce { copy(suggestedShops = RamenShops(suggestedShops + page.shops), nextCursor = page.nextCursor) }
            },
            onError = { error ->
                analytics.shopLoadFailed("suggestions", error)
                reduce { copy(hasSuggestionsError = true) }
            },
        )
    }

    private suspend fun fetchSuggestions(): RamapResult<ShopSuggestionsPage> {
        val savedIds = (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value?.bookmarkedShopIds.orEmpty()
        val savedShops = if (savedIds.isEmpty()) RamapResult.Success(RamenShops(emptyMap())) else shopRepository.fetchRamenShops(savedIds)
        if (savedShops is RamapResult.Error) return savedShops
        val suggestions = suggestionsRepository.fetchSuggestions(cursor = null, limit = SUGGESTION_LIMIT)
        if (suggestions is RamapResult.Error) return suggestions
        val page = (suggestions as RamapResult.Success).data
        return RamapResult.Success(
            page.copy(shops = RamenShops((savedShops as RamapResult.Success).data + page.shops)),
        )
    }

    private fun loadNextPage(isRetry: Boolean = false) {
        val cursor = currentState.nextCursor ?: return
        if (!currentState.canLoadNextPage || (currentState.hasNextPageError && !isRetry)) return
        launchResultTask(
            taskKey = NEXT_PAGE_TASK,
            loadKey = ShopSelectionLoadKey.NextPage,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(hasNextPageError = false) },
            request = { suggestionsRepository.fetchSuggestions(cursor = cursor, limit = SUGGESTION_LIMIT) },
            onSuccess = { page ->
                analytics.shopLoadResult("next_page", page.shops.size)
                reduce {
                    copy(
                        suggestedShops = RamenShops(suggestedShops + page.shops),
                        nextCursor = page.nextCursor,
                    )
                }
            },
            onError = { error ->
                analytics.shopLoadFailed("next_page", error)
                reduce { copy(hasNextPageError = true) }
            },
        )
    }

    private fun changeQuery(query: String) {
        cancelTask(SEARCH_TASK)
        cancelTask(NEXT_PAGE_TASK)
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
                if (debounce) delay(SEARCH_DEBOUNCE_MS.milliseconds)
                searchShops(query)
            },
            onSuccess = { shops ->
                analytics.shopLoadResult("search", shops.size)
                reduce { copy(searchResults = shops) }
            },
            onError = { error ->
                analytics.shopLoadFailed("search", error)
                reduce { copy(hasSearchError = true) }
            },
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
            request = { personalizationStore.updateBookmark(shopId, enabled) },
            onSuccess = {
                analytics.bookmarkResult(enabled = enabled, savedCount = savedShopCount())
                reduce { copy(suggestedShops = RamenShops(suggestedShops + (shopId to shop))) }
            },
            onError = { error ->
                analytics.bookmarkResult(enabled = enabled, savedCount = savedShopCount(), error = error)
                postSideEffect(
                    ShopSelectionSideEffect.ShowToast(
                        ToastData(message = Res.string.personalization_update_failure_message, type = ToastType.ERROR),
                    ),
                )
            },
        )
    }

    private fun savedShopCount(): Int =
        (personalizationStore.state.value as? PersonalizationBootstrapState.Success)?.value?.bookmarkedShopIds?.size
            ?: currentState.bookmarks.savedCount

    private fun isCurrentBookmarkOwner(): Boolean {
        if (bookmarkOwnerAccountId == loginRepository.currentAccountId()) return true
        loadPersonalization()
        return false
    }

    private fun complete() {
        if (!currentState.canComplete || !isCurrentBookmarkOwner()) return
        val savedCount = currentState.bookmarks.savedCount
        launchTask(
            taskKey = COMPLETE_TASK,
            loadKey = ShopSelectionLoadKey.Complete,
            policy = TaskPolicy.IgnoreNew,
        ) {
            try {
                storage.saveShopSelection(savedCount)
                cancelTask(OBSERVE_PERSONALIZATION_TASK)
                cancelTask(SEARCH_TASK)
                cancelTask(SUGGESTIONS_TASK)
                cancelTask(NEXT_PAGE_TASK)
                reduce { copy(isCompleted = true) }
                postSideEffect(ShopSelectionSideEffect.Completed(savedCount))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                handleError(exception)
                postSideEffect(
                    ShopSelectionSideEffect.ShowToast(
                        ToastData(message = Res.string.onboarding_completion_failure, type = ToastType.ERROR),
                    ),
                )
            }
        }
    }

    private companion object {
        const val PERSONALIZATION_TASK = "onboarding-personalization"
        const val OBSERVE_PERSONALIZATION_TASK = "onboarding-observe-personalization"
        const val SUGGESTIONS_TASK = "onboarding-suggestions"
        const val NEXT_PAGE_TASK = "onboarding-next-page"
        const val SEARCH_TASK = "onboarding-search"
        const val BOOKMARK_TASK = "onboarding-bookmark"
        const val COMPLETE_TASK = "onboarding-complete"
        const val SUGGESTION_LIMIT = 20
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
