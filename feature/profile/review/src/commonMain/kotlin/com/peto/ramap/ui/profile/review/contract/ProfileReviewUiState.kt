package com.peto.ramap.ui.profile.review.contract

import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ProfileReviewUiState(
    val reviews: List<Review> = emptyList(),
    val hasMore: Boolean = true,
    val loadFailed: Boolean = false,
    val profile: PublicProfile? = null,
    val isAuthenticated: Boolean = false,
    val currentUserId: String? = null,
    val blockedUsers: List<PublicProfile> = emptyList(),
    val isBlocked: Boolean = false,
    val reportTargetId: String? = null,
    val reportingUser: Boolean = false,
    val blockTarget: PublicProfile? = null,
    val deleteTarget: Review? = null,
    val likingReviewId: String? = null,
    val actionFailed: Boolean = false,
    val actionSucceeded: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ProfileReviewUiState> {
    val isLoading: Boolean get() = loadState.isLoading(ProfileReviewLoadKey.Feed)
    val isActing: Boolean get() = loadState.isLoading(ProfileReviewLoadKey.Action)
    val isDeleting: Boolean get() = loadState.isLoading(ProfileReviewLoadKey.Delete)
    val isLiking: Boolean get() = loadState.isLoading(ProfileReviewLoadKey.Like)

    override fun withLoadingState(loadState: LoadState): ProfileReviewUiState = copy(loadState = loadState)
}
