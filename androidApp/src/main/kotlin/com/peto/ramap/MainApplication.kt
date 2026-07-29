package com.peto.ramap

import android.app.Application
import android.content.pm.ApplicationInfo
import co.touchlab.kermit.Logger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.common.KakaoSdk
import com.naver.maps.map.NaverMapSdk
import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.attribution.InstallReferrerAttributor
import com.peto.ramap.di.initKoin
import com.peto.ramap.network.config.RamapSecrets
import org.koin.android.ext.koin.androidContext
import org.koin.java.KoinJavaComponent.get

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        configureFirebase()
        Logger.setLogWriters(listOf(CrashlyticsLogWriter()))
        FirebaseMessaging.getInstance().register()
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NcpKeyClient(RamapSecrets.naverMapNcpKeyId)
        KakaoSdk.init(this, RamapSecrets.kakaoNativeAppKey)
        initKoin {
            androidContext(this@MainApplication)
        }
        InstallReferrerAttributor(this, get(AnalyticsTracker::class.java))
            .collectOnce()
    }

    private fun configureFirebase() {
        val isDebug = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(false)
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = false
        }
    }
}
