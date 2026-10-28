package com.peto.ramap.ui.review.search.di

import com.peto.ramap.ui.review.search.ReviewShopSearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewSearchModule =
    module {
        viewModelOf(::ReviewShopSearchViewModel)
    }
