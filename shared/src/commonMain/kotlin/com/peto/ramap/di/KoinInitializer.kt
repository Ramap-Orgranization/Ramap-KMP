package com.peto.ramap.di

import com.peto.ramap.data.datasource.di.dataSourceModule
import com.peto.ramap.data.repository.di.repositoryModule
import com.peto.ramap.designsystem.di.designSystemModule
import com.peto.ramap.network.di.networkModule
import com.peto.ramap.notification.di.notificationModule
import com.peto.ramap.platform.di.platformModule
import com.peto.ramap.ui.hidden.di.hiddenModule
import com.peto.ramap.ui.main.event.di.eventDetailModule
import com.peto.ramap.ui.main.event.list.di.eventListModule
import com.peto.ramap.ui.main.map.di.mapModule
import com.peto.ramap.ui.main.my.di.myTabModule
import com.peto.ramap.ui.settings.notification.di.notificationSettingsModule
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
            mapModule,
            myTabModule,
            hiddenModule,
            eventDetailModule,
            eventListModule,
            notificationModule,
            notificationSettingsModule,
        )
    }
}
