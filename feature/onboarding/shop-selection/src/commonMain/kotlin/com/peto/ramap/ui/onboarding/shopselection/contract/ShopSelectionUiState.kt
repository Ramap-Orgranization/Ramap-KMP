package com.peto.ramap.ui.onboarding.shopselection.contract

import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.domain.model.onboarding.ShopSuggestionsCursor
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

public data class ShopSelectionUiState(
    val query: String = "",
    val suggestedShops: RamenShops = RamenShops(emptyMap()),
    val nextCursor: ShopSuggestionsCursor? = null,
    val searchResults: RamenShops = RamenShops(emptyMap()),
    val bookmarks: OnboardingBookmarks = OnboardingBookmarks(emptySet()),
    val hasPersonalization: Boolean = false,
    val hasPersonalizationError: Boolean = false,
    val hasSuggestionsError: Boolean = false,
    val hasNextPageError: Boolean = false,
    val hasSearchError: Boolean = false,
    val isCompleted: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ShopSelectionUiState> {
    val isSearching: Boolean get() = query.isNotBlank()
    val shops: RamenShops get() = if (isSearching) searchResults else suggestedShops
    val hasShopsError: Boolean get() = if (isSearching) hasSearchError else hasSuggestionsError
    val isLoadingShops: Boolean get() = loadState.isLoading(if (isSearching) ShopSelectionLoadKey.Search else ShopSelectionLoadKey.Suggestions)
    val isLoadingNextPage: Boolean get() = loadState.isLoading(ShopSelectionLoadKey.NextPage)
    val canLoadNextPage: Boolean get() = nextCursor != null && !isSearching && hasPersonalization && !hasPersonalizationError && !loadState.isLoading(ShopSelectionLoadKey.Personalization) && !hasSuggestionsError && !isLoadingShops && !isLoadingNextPage && !isCompleting && !isCompleted
    val showsNextPageFooter: Boolean get() = !isSearching && (nextCursor != null || isLoadingNextPage || hasNextPageError)
    val canChangeBookmarks: Boolean get() = hasPersonalization && !loadState.isLoading(ShopSelectionLoadKey.Personalization) && !loadState.isLoading(ShopSelectionLoadKey.BookmarkUpdates) && !isCompleting && !isCompleted
    val isCompleting: Boolean get() = loadState.isLoading(ShopSelectionLoadKey.Complete)
    val canComplete: Boolean get() = canChangeBookmarks && bookmarks.canComplete

    override fun withLoadingState(loadState: LoadState): ShopSelectionUiState = copy(loadState = loadState)
}
