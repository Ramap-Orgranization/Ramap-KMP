package com.peto.ramap.ui.main.my.di

import com.peto.ramap.ui.main.my.MyTabViewModel
import org.koin.dsl.module

val myTabModule =
    module {
        factory {
            MyTabViewModel(get(), get(), get(), get(), get())
        }
    }
