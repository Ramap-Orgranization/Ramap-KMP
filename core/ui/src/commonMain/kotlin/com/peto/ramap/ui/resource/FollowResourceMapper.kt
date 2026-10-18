package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.FollowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow
import ramap.shared.generated.resources.follow_approve
import ramap.shared.generated.resources.follow_cancel_request
import ramap.shared.generated.resources.follow_empty
import ramap.shared.generated.resources.follow_followers
import ramap.shared.generated.resources.follow_followers_empty
import ramap.shared.generated.resources.follow_following
import ramap.shared.generated.resources.follow_following_empty
import ramap.shared.generated.resources.follow_reject
import ramap.shared.generated.resources.follow_remove
import ramap.shared.generated.resources.follow_requests
import ramap.shared.generated.resources.ic_follow_add
import ramap.shared.generated.resources.ic_follow_check
import ramap.shared.generated.resources.ic_follow_pending
import ramap.shared.generated.resources.unfollow

object FollowResourceMapper {
    fun stateIcon(state: FollowState): DrawableResource =
        when (state) {
            FollowState.NONE -> Res.drawable.ic_follow_add
            FollowState.PENDING -> Res.drawable.ic_follow_pending
            FollowState.FOLLOWING -> Res.drawable.ic_follow_check
        }

    fun listLabel(list: FollowList): StringResource =
        when (list) {
            FollowList.FOLLOWERS -> Res.string.follow_followers
            FollowList.FOLLOWING -> Res.string.follow_following
            FollowList.REQUESTS -> Res.string.follow_requests
        }

    fun emptyLabel(list: FollowList): StringResource =
        when (list) {
            FollowList.FOLLOWERS -> Res.string.follow_followers_empty
            FollowList.FOLLOWING -> Res.string.follow_following_empty
            FollowList.REQUESTS -> Res.string.follow_empty
        }

    fun stateActionLabel(state: FollowState): StringResource =
        when (state) {
            FollowState.NONE -> Res.string.follow
            FollowState.PENDING -> Res.string.follow_cancel_request
            FollowState.FOLLOWING -> Res.string.unfollow
        }

    fun actionLabel(action: FollowAction): StringResource =
        when (action) {
            FollowAction.FOLLOW -> Res.string.follow
            FollowAction.UNFOLLOW -> Res.string.unfollow
            FollowAction.APPROVE -> Res.string.follow_approve
            FollowAction.REJECT -> Res.string.follow_reject
            FollowAction.REMOVE -> Res.string.follow_remove
        }
}
