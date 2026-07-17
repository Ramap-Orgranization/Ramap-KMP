package com.peto.ramap

import com.peto.ramap.data.datasource.di.dataSourceModule
import com.peto.ramap.data.repository.di.repositoryModule
import com.peto.ramap.designsystem.di.designSystemModule
import com.peto.ramap.network.di.networkModule
import com.peto.ramap.notification.di.notificationModule
import com.peto.ramap.platform.di.platformModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            appModule,
            networkModule,
            designSystemModule,
            platformModule,
            dataSourceModule,
            repositoryModule,
            notificationModule,
        )
    }
}
