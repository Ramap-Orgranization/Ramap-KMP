package com.peto.ramap.ui.main.newshops.di

import com.peto.ramap.ui.main.newshops.NewShopsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

val newShopsModule =
    module {
        viewModelOf(::NewShopsViewModel)
        single<Clock> { Clock.System }
    }
