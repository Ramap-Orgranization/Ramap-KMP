package com.peto.ramap

import android.app.Application
import com.naver.maps.map.NaverMapSdk
import com.peto.ramap.core.config.RamapAppConfig
import com.peto.ramap.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NcpKeyClient(RamapAppConfig.naverMapNcpKeyId)
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
