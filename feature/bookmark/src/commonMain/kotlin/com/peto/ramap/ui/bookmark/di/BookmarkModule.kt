package com.peto.ramap.ui.bookmark.di

import com.peto.ramap.ui.bookmark.BookmarkedShopListViewModel
import org.koin.dsl.module

val bookmarkModule =
    module {
        factory { BookmarkedShopListViewModel(get(), get(), get()) }
    }
