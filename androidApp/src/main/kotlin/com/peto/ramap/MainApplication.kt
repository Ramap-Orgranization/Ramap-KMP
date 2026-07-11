package com.peto.ramap

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.peto.ramap.core.config.RamapAppConfig
import com.peto.ramap.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoMapSdk.init(this, RamapAppConfig.kakaoNativeAppKey)
        KakaoSdk.init(this, RamapAppConfig.kakaoNativeAppKey)
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
