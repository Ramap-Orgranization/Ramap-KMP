package com.peto.ramap.di

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter
import com.peto.ramap.analytics.FirebaseAnalyticsTracker
import com.peto.ramap.analytics.FirebaseCrashReporter
import com.peto.ramap.platform.AndroidAppSettingsOpener
import com.peto.ramap.platform.AndroidAppVersionProvider
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.location.AndroidCurrentLocationProvider
import com.peto.ramap.platform.location.CurrentLocationProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            AndroidAppSettingsOpener(androidContext())
        }
        single<AppVersionProvider> { AndroidAppVersionProvider(androidContext()) }
        single<CurrentLocationProvider> { AndroidCurrentLocationProvider(androidContext()) }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<CrashReporter> { FirebaseCrashReporter() }
    }
