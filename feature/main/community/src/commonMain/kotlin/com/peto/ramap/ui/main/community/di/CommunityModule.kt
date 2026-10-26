package com.peto.ramap.ui.main.community.di

import com.peto.ramap.ui.main.community.CommunityViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val communityModule =
    module {
        viewModelOf(::CommunityViewModel)
    }
