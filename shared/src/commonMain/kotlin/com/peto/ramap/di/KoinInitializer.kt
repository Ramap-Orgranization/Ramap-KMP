package com.peto.ramap.di

import com.peto.ramap.data.datasource.di.dataSourceModule
import com.peto.ramap.data.repository.di.repositoryModule
import com.peto.ramap.data.usecase.di.useCaseModule
import com.peto.ramap.designsystem.di.designSystemModule
import com.peto.ramap.network.di.networkModule
import com.peto.ramap.notification.di.notificationModule
import com.peto.ramap.ui.di.uiModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            appModule,
            networkModule,
            designSystemModule,
            uiModule,
            platformModule,
            dataSourceModule,
            repositoryModule,
            useCaseModule,
            notificationModule,
        )
    }
}
