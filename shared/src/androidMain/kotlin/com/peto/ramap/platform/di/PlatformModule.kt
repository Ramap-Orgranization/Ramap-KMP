package com.peto.ramap.platform.di

import com.peto.ramap.platform.AndroidAppSettingsOpener
import com.peto.ramap.platform.AndroidAppVersionProvider
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.AppVersionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            AndroidAppSettingsOpener(androidContext())
        }
        single<AppVersionProvider> { AndroidAppVersionProvider(androidContext()) }
    }
