package com.peto.ramap.di

import com.peto.ramap.data.datasource.di.dataSourceModule
import com.peto.ramap.data.repository.di.repositoryModule
import com.peto.ramap.designsystem.di.designSystemModule
import com.peto.ramap.platform.di.platformModule
import com.peto.ramap.ui.login.di.loginModule
import com.peto.ramap.ui.map.di.mapModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            networkModule,
            designSystemModule,
            platformModule,
            dataSourceModule,
            repositoryModule,
            mapModule,
            loginModule,
        )
    }
}
