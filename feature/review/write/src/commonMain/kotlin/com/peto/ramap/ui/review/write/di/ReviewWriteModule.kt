package com.peto.ramap.ui.review.write.di

import com.peto.ramap.ui.review.write.ReviewWriteViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewWriteModule =
    module {
        viewModelOf(::ReviewWriteViewModel)
    }
