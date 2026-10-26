package com.peto.ramap.ui.main.feed.di

import com.peto.ramap.ui.main.feed.FeedViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val feedModule =
    module {
        viewModelOf(::FeedViewModel)
    }
