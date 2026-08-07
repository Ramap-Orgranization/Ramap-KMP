package com.peto.ramap.ui.bookmark.list.di

import com.peto.ramap.ui.bookmark.list.BookmarkedShopListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bookmarkListModule =
    module {
        viewModelOf(::BookmarkedShopListViewModel)
    }
