package com.peto.ramap.ui.profile.follow

import com.peto.ramap.ui.base.SideEffect

sealed interface FollowEffect : SideEffect {
    data object ActionFailed : FollowEffect
}
