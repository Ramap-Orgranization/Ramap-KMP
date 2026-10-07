package com.peto.ramap.ui.profile.follow.di

import com.peto.ramap.ui.profile.follow.FollowViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val followModule =
    module {
        viewModelOf(::FollowViewModel)
    }
