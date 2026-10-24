package com.peto.ramap.ui.profile.follow.contract

import com.peto.ramap.ui.base.SideEffect

sealed interface FollowEffect : SideEffect {
    data object ActionFailed : FollowEffect
}
