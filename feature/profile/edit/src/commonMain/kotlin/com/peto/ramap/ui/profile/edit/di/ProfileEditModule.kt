package com.peto.ramap.ui.profile.edit.di

import com.peto.ramap.ui.profile.edit.ProfileEditViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileEditModule =
    module {
        viewModelOf(::ProfileEditViewModel)
    }
