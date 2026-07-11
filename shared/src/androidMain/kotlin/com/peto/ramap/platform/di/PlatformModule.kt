package com.peto.ramap.platform.di

import com.peto.ramap.data.auth.AndroidKakaoLoginProvider
import com.peto.ramap.data.auth.KakaoLoginProvider
import com.peto.ramap.platform.AndroidAppSettingsOpener
import com.peto.ramap.platform.AppSettingsOpener
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule =
    module {
        single<AppSettingsOpener> {
            AndroidAppSettingsOpener(androidContext())
        }
        single<KakaoLoginProvider> {
            AndroidKakaoLoginProvider()
        }
    }
