package com.peto.ramap.debug.admin.di

import com.peto.ramap.debug.admin.data.datasource.AdminRegistrationDataSource
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val adminModule =
    module {
        singleOf(::AdminRegistrationDataSource)
        viewModelOf(::AdminRegistrationViewModel)
    }
