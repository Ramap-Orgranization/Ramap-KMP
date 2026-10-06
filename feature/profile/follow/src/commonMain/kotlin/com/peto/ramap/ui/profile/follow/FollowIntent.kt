package com.peto.ramap.ui.profile.follow

import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.ui.base.Intent

sealed interface FollowIntent : Intent {
    data class Select(
        val list: FollowList,
    ) : FollowIntent

    data class Change(
        val userId: String,
        val action: FollowAction,
    ) : FollowIntent

    data object Retry : FollowIntent

    data object LoadMore : FollowIntent

    data object ReturnedFromProfile : FollowIntent
}
