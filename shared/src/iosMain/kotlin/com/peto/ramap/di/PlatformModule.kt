package com.peto.ramap.di

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter
import com.peto.ramap.analytics.FirebaseAnalyticsTracker
import com.peto.ramap.analytics.FirebaseCrashReporter
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.IosAppSettingsOpener
import com.peto.ramap.platform.IosAppVersionProvider
import com.peto.ramap.platform.createAppNoticeStorage
import com.peto.ramap.platform.createMapSearchHistoryStorage
import com.peto.ramap.platform.location.CurrentLocationProvider
import com.peto.ramap.platform.location.IosCurrentLocationProvider
import com.peto.ramap.platform.network.IosNetworkConnectivityObserver
import com.peto.ramap.platform.network.NetworkConnectivityObserver
import com.peto.ramap.platform.storage.AppNoticeStorage
import com.peto.ramap.platform.storage.SearchHistoryStorage
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            IosAppSettingsOpener()
        }
        single<AppVersionProvider> { IosAppVersionProvider() }
        single<AppNoticeStorage> { createAppNoticeStorage() }
        single<SearchHistoryStorage> { createMapSearchHistoryStorage() }
        single<NetworkConnectivityObserver> { IosNetworkConnectivityObserver() }
        single<CurrentLocationProvider> { IosCurrentLocationProvider() }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<CrashReporter> { FirebaseCrashReporter() }
    }
