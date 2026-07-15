package com.peto.ramap

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMapSdk
import com.peto.ramap.core.config.RamapAppConfig
import com.peto.ramap.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().register()
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NcpKeyClient(RamapAppConfig.naverMapNcpKeyId)
        KakaoSdk.init(this, RamapAppConfig.kakaoNativeAppKey)
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
