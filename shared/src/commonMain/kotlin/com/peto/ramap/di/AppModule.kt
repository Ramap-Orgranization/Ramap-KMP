package com.peto.ramap.di

import com.peto.ramap.analytics.di.analyticsModule
import com.peto.ramap.deeplink.DeepLinkEntryPoint
import com.peto.ramap.log.AppAnalytics
import com.peto.ramap.navigation.deeplink.ShopDeepLinkDispatcher
import com.peto.ramap.navigation.deeplink.ShopDeepLinkParser
import com.peto.ramap.navigation.deeplink.ShopLinkConfig
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.network.config.RamapSecrets
import com.peto.ramap.ui.account.di.accountModule
import com.peto.ramap.ui.bookmark.importation.di.importationModule
import com.peto.ramap.ui.bookmark.list.di.bookmarkListModule
import com.peto.ramap.ui.hidden.di.hiddenModule
import com.peto.ramap.ui.main.event.detail.di.eventDetailModule
import com.peto.ramap.ui.main.event.list.di.eventsModule
import com.peto.ramap.ui.main.map.di.mapModule
import com.peto.ramap.ui.main.my.di.settingsModule
import com.peto.ramap.ui.main.ranking.di.rankingModule
import com.peto.ramap.ui.notification.di.notificationSettingsModule
import com.peto.ramap.ui.report.di.reportModule
import com.peto.ramap.ui.subscribed.di.subscribedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal val appModule =
    module {
        includes(
            accountModule,
            bookmarkListModule,
            eventDetailModule,
            eventsModule,
            hiddenModule,
            mapModule,
            importationModule,
            rankingModule,
            notificationSettingsModule,
            reportModule,
            settingsModule,
            subscribedModule,
            analyticsModule,
        )
        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        singleOf(::AppAnalytics)
        singleOf(::DeepLinkEntryPoint)
        single {
            ShopLinkConfig(
                baseUrl = RamapSecrets.shopLinkBaseUrl,
                webHost = RamapSecrets.shopLinkWebHost,
            )
        }
        singleOf(::ShopShareLinkFactory)
        singleOf(::ShopDeepLinkParser)
        singleOf(::ShopDeepLinkDispatcher)
    }
