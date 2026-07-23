package com.peto.ramap.di

import com.peto.ramap.ui.account.di.accountModule
import com.peto.ramap.ui.bookmark.di.bookmarkModule
import com.peto.ramap.ui.hidden.di.hiddenModule
import com.peto.ramap.ui.main.event.di.eventDetailModule
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
import org.koin.dsl.module

internal val appModule =
    module {
        includes(
            accountModule,
            bookmarkModule,
            eventDetailModule,
            eventsModule,
            hiddenModule,
            mapModule,
            rankingModule,
            notificationSettingsModule,
            reportModule,
            settingsModule,
            subscribedModule,
        )
        single<CoroutineScope> {
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    }
