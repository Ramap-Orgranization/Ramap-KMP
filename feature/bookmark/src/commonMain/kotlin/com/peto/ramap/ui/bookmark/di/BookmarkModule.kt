package com.peto.ramap.ui.bookmark.di

import com.peto.ramap.ui.bookmark.BookmarkedShopListAnalytics
import com.peto.ramap.ui.bookmark.BookmarkedShopListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookmarkModule =
    module {
        viewModelOf(::BookmarkedShopListViewModel)
        singleOf(::BookmarkedShopListAnalytics)
    }
