package com.peto.ramap.di

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.analytics.CrashReporter
import com.peto.ramap.analytics.FirebaseAnalyticsTracker
import com.peto.ramap.analytics.FirebaseCrashReporter
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import com.peto.ramap.platform.IosAppSettingsOpener
import com.peto.ramap.platform.IosAppVersionProvider
import com.peto.ramap.platform.location.CurrentLocationProvider
import com.peto.ramap.platform.location.IosCurrentLocationProvider
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            IosAppSettingsOpener()
        }
        single<AppVersionProvider> { IosAppVersionProvider() }
        single<CurrentLocationProvider> { IosCurrentLocationProvider() }
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
        single<CrashReporter> { FirebaseCrashReporter() }
    }
