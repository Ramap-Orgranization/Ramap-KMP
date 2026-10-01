package com.peto.ramap.ui.review.my.di

import com.peto.ramap.ui.review.my.MyReviewsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewMyModule =
    module {
        viewModelOf(::MyReviewsViewModel)
    }
