package com.peto.ramap.platform.di

import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.IosAppSettingsOpener
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            IosAppSettingsOpener()
        }
    }
