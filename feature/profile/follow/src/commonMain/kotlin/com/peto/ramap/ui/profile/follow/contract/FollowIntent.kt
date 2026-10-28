package com.peto.ramap.ui.profile.follow.contract

import com.peto.ramap.domain.model.follow.FollowAction
import com.peto.ramap.domain.model.follow.FollowList
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
