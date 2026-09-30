package com.peto.ramap.debug.admin.di

import com.peto.ramap.debug.admin.data.datasource.AdminAccessDataSource
import com.peto.ramap.debug.admin.data.datasource.AdminRegistrationDataSource
import com.peto.ramap.debug.admin.data.datasource.RemoteAdminAccessDataSource
import com.peto.ramap.debug.admin.ui.login.AdminLoginViewModel
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val adminModule =
    module {
        single<AdminAccessDataSource> { RemoteAdminAccessDataSource(get()) }
        singleOf(::AdminRegistrationDataSource)
        viewModelOf(::AdminLoginViewModel)
        viewModelOf(::AdminRegistrationViewModel)
    }
