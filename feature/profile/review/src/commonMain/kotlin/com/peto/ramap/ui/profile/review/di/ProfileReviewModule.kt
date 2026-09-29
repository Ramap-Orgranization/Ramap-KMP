package com.peto.ramap.ui.profile.review.di

import com.peto.ramap.ui.profile.review.ProfileReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileReviewModule =
    module {
        viewModelOf(::ProfileReviewViewModel)
    }
