package com.peto.ramap.di

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter
import com.peto.ramap.analytics.FirebaseAnalyticsTracker
import com.peto.ramap.analytics.FirebaseCrashReporter
import com.peto.ramap.data.datasource.event.EventReadDataSource
import com.peto.ramap.data.datasource.event.createEventReadDataSource
import com.peto.ramap.domain.storage.SearchHistoryStorage
import com.peto.ramap.platform.AndroidAppSettingsOpener
import com.peto.ramap.platform.AndroidAppVersionProvider
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.createAppNoticeStorage
import com.peto.ramap.platform.createMapSearchHistoryStorage
import com.peto.ramap.platform.location.AndroidCurrentLocationProvider
import com.peto.ramap.platform.location.CurrentLocationProvider
import com.peto.ramap.platform.network.AndroidNetworkConnectivityObserver
import com.peto.ramap.platform.network.NetworkConnectivityObserver
import com.peto.ramap.platform.storage.AppNoticeStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            AndroidAppSettingsOpener(androidContext())
        }
        single<AppVersionProvider> { AndroidAppVersionProvider(androidContext()) }
        single<EventReadDataSource> { createEventReadDataSource(androidContext()) }
        single<AppNoticeStorage> { createAppNoticeStorage(androidContext()) }
        single<SearchHistoryStorage> { createMapSearchHistoryStorage(androidContext()) }
        single<NetworkConnectivityObserver> { AndroidNetworkConnectivityObserver(androidContext()) }
        single<CurrentLocationProvider> { AndroidCurrentLocationProvider(androidContext()) }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<CrashReporter> { FirebaseCrashReporter() }
    }
