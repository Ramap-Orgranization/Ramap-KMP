package com.peto.ramap.ui.main

import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewState
import com.peto.ramap.ui.shop.review.contract.ReviewsUiState

data class ShopSectionsSnapshot(
    val map: MapUiState,
    val overview: ShopOverviewState = ShopOverviewState(),
    val menuState: ShopMenuUiState = ShopMenuUiState(),
    val reviewState: ReviewsUiState = ReviewsUiState(),
) {
    val currentDateTime get() = map.currentDateTime
    val selectedShopId get() = map.selectedShopId
    val loadState get() = map.loadState
    val shops get() = map.shops
    val selectedShopFocusRequest get() = map.selectedShopFocusRequest
    val search get() = map.search
    val recentSearches get() = map.recentSearches
    val recentlyViewedShops get() = map.recentlyViewedShops
    val filters get() = map.filters
    val operatingNotices get() = map.operatingNotices
    val bounds get() = map.bounds
    val hasViewportLoadFailed get() = map.hasViewportLoadFailed
    val cameraPosition get() = map.cameraPosition
    val currentLocation get() = map.currentLocation
    val locationFocusStatus get() = map.locationFocusStatus
    val bookmarkedShopIds get() = map.bookmarkedShopIds
    val notificationShopIds get() = map.notificationShopIds
    val hiddenShopIds get() = map.hiddenShopIds
    val isBookmarkedView get() = map.isBookmarkedView
    val isLoggedIn get() = map.isLoggedIn
    val isSearchLoading get() = map.isSearchLoading
    val showsLoadingOverlay get() = map.showsLoadingOverlay
    val searchResultShops get() = map.searchResultShops
    val hasSearchResult get() = map.hasSearchResult
    val searchResultGuide get() = map.searchResultGuide
    val showSearchResults get() = map.showSearchResults
    val showBottomSheet get() = map.selectedShopId != null
    val focusShops get() = map.focusShops
    val shouldFocusSelectedShop get() = map.shouldFocusSelectedShop
    val shouldFocusNearestSearchResult get() = map.shouldFocusNearestSearchResult
    val focusRequestKey get() = map.focusRequestKey
    val initialFocusLocation get() = map.initialFocusLocation
    val shouldBootstrapLocationFocusStatus get() = map.shouldBootstrapLocationFocusStatus
    val markerShops get() = map.markerShops
    val selectedShop get() = map.selectedShop
    val shopWaiting get() = overview.shopWaiting
    val currentUserId get() = reviewState.currentUserId
    val currentProfileIsPublic get() = reviewState.currentProfileIsPublic
    val overviewState get() = overview.overviewState
    val actingReviewId get() = reviewState.actingReviewId
    val revealedBlockedReviews get() = reviewState.revealedBlockedReviews
    val revealingBlockedReviewId get() = reviewState.revealingBlockedReviewId
    val pendingUnblockReview get() = reviewState.pendingUnblockReview
    val reportReview get() = reviewState.reportReview
    val hasMoreShopReviews get() = reviewState.hasMoreReviews
    val isReportingReview get() = reviewState.isReportingReview
    val isUnblockingReview get() = reviewState.isUnblockingReview
    val isLoadingMoreShopReviews get() = reviewState.isLoadingMoreReviews
    val isRetryingShopReviews get() = reviewState.isLoading
    val shopOverview get() = overview.shopOverview
    val isShopDetailLoading get() = overview.isLoading
    val hasShopDetailLoadFailed get() = overview.hasLoadFailure
    val shopMenu get() = menuState.menu
    val shopMenuShopId get() = menuState.shopId
    val hasShopMenuLoadFailure get() = menuState.hasLoadFailure
}
