package com.peto.ramap.ui.shop.review.di

import com.peto.ramap.ui.shop.review.ReviewsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopReviewModule =
    module {
        viewModelOf(::ReviewsViewModel)
    }
