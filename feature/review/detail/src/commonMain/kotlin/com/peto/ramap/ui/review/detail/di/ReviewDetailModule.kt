package com.peto.ramap.ui.review.detail.di

import com.peto.ramap.ui.review.detail.ReviewDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reviewDetailModule =
    module {
        viewModel { (reviewId: String, showDetail: Boolean) ->
            ReviewDetailViewModel(reviewId, showDetail, get(), get(), get(), get(), get(), get())
        }
    }
