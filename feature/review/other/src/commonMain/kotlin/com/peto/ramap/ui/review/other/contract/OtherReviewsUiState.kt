package com.peto.ramap.ui.review.other.contract

import com.peto.ramap.domain.model.community.ProfileAccess
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class OtherReviewsUiState(
    val userId: String? = null,
    val currentUserId: String? = null,
    val sessionResolved: Boolean = false,
    val profileAccess: ProfileAccess? = null,
    val reviews: List<Review> = emptyList(),
    val failed: Boolean = false,
    val hasMore: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<OtherReviewsUiState> {
    val loading: Boolean get() = loadState.isLoading(OtherReviewsLoadKey.Page)
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
    val showsInitialLoading: Boolean get() = loading && reviews.isEmpty()
    val showsAppendLoading: Boolean get() = loading && reviews.isNotEmpty()
    val showsUnavailableProfile: Boolean
        get() = !failed && (profileAccess == ProfileAccess.Private || profileAccess == ProfileAccess.Unavailable)
    val showsBlockedContent: Boolean get() = loaded && !failed && isBlocked
    val showsEmptyReviews: Boolean get() = !failed && profileAccess is ProfileAccess.Visible && reviews.isEmpty()

    override fun withLoadingState(loadState: LoadState): OtherReviewsUiState = copy(loadState = loadState)
}
