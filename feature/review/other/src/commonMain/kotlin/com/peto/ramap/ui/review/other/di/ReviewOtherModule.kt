package com.peto.ramap.ui.review.other.di

import com.peto.ramap.ui.review.other.OtherReviewsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewOtherModule =
    module {
        viewModelOf(::OtherReviewsViewModel)
    }
