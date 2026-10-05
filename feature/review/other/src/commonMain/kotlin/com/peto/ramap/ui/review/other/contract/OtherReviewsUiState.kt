package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class OtherReviewsUiState(
    val userId: String? = null,
    val currentUserId: String? = null,
    val sessionResolved: Boolean = false,
    val profileAccess: ProfileAccess? = null,
    val reviews: List<Review> = emptyList(),
    val reviewsOffset: Long = 0L,
    val selectedTab: OtherReviewsTab = OtherReviewsTab.Reviews,
    val savedShops: List<RamenShop> = emptyList(),
    val savedShopsLoaded: Boolean = false,
    val savedShopsFailed: Boolean = false,
    val savedShopsHasMore: Boolean = false,
    val savedShopsOffset: Long = 0L,
    val failed: Boolean = false,
    val hasMore: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<OtherReviewsUiState> {
    val loading: Boolean get() = loadState.isLoading(OtherReviewsLoadKey.Page)
    val savedShopsLoading: Boolean get() = loadState.isLoading(OtherReviewsLoadKey.SavedShops)
    val activeLoading: Boolean
        get() = if (selectedTab == OtherReviewsTab.Reviews || profileAccess == null) loading else savedShopsLoading
    val activeFailed: Boolean
        get() = if (selectedTab == OtherReviewsTab.Reviews || profileAccess == null) failed else savedShopsFailed
    val activeHasMore: Boolean
        get() = if (selectedTab == OtherReviewsTab.Reviews) hasMore else savedShopsHasMore
    val activeEmpty: Boolean
        get() = if (selectedTab == OtherReviewsTab.Reviews) reviews.isEmpty() else savedShops.isEmpty()
    val showsEmptySavedShops: Boolean
        get() =
            selectedTab == OtherReviewsTab.SavedShops &&
                savedShopsLoaded &&
                !savedShopsLoading &&
                !savedShopsFailed &&
                savedShops.isEmpty() &&
                profileAccess is ProfileAccess.Visible
    val blocking: Boolean get() = loadState.isLoading(OtherReviewsLoadKey.Block)

    val profile: PublicProfile?
        get() =
            when (val access = profileAccess) {
                is ProfileAccess.Visible -> access.profile
                is ProfileAccess.Blocked -> access.profile
                else -> null
            }
    val isBlocked: Boolean get() = profileAccess is ProfileAccess.Blocked
    val isPrivate: Boolean get() = profileAccess == ProfileAccess.Private
    val loaded: Boolean get() = profileAccess != null || failed

    val showsProfileHeader: Boolean get() = profile != null
    val showsBlockAction: Boolean
        get() = profileAccess is ProfileAccess.Visible && profile?.userId != currentUserId
    val showsSummaryHeader: Boolean get() = profileAccess is ProfileAccess.Visible && reviews.isNotEmpty()
    val showsInitialLoading: Boolean get() = activeLoading && activeEmpty
    val showsAppendLoading: Boolean get() = activeLoading && !activeEmpty
    val showsUnavailableProfile: Boolean
        get() = !failed && (profileAccess == ProfileAccess.Private || profileAccess == ProfileAccess.Unavailable)
    val showsBlockedContent: Boolean get() = loaded && !failed && isBlocked
    val showsEmptyReviews: Boolean
        get() =
            selectedTab == OtherReviewsTab.Reviews &&
                !loading &&
                !failed &&
                profileAccess is ProfileAccess.Visible &&
                reviews.isEmpty()

    override fun withLoadingState(loadState: LoadState): OtherReviewsUiState = copy(loadState = loadState)
}
