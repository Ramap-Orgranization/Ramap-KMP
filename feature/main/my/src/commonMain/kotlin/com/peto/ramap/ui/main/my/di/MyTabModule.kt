package com.peto.ramap.ui.main.my.di

import com.peto.ramap.ui.main.my.MyTabViewModel
import com.peto.ramap.ui.main.my.profile.MyProfileViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val myTabModule =
    module {
        viewModelOf(::MyTabViewModel)
        viewModelOf(::MyProfileViewModel)
    }
