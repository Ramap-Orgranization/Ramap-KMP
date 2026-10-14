package com.peto.ramap.ui.shop.review.di

import com.peto.ramap.ui.shop.review.ShopReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val shopReviewModule =
    module {
        viewModelOf(::ShopReviewViewModel)
    }
