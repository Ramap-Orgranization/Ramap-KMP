package com.peto.ramap

import android.app.Application
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMapSdk
import com.peto.ramap.di.initKoin
import com.peto.ramap.network.config.RamapSecrets
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().register()
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NcpKeyClient(RamapSecrets.naverMapNcpKeyId)
        KakaoSdk.init(this, RamapSecrets.kakaoNativeAppKey)
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
