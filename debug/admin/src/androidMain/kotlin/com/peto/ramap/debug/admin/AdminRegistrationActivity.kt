package com.peto.ramap.debug.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.peto.ramap.data.auth.KakaoLoginActivityProvider
import com.peto.ramap.debug.admin.di.adminModule
import com.peto.ramap.debug.admin.ui.login.AdminLoginRoute
import com.peto.ramap.theme.RamapTheme
import org.koin.core.context.loadKoinModules

class AdminRegistrationActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        KakaoLoginActivityProvider.attach(this)
    }

    override fun onPause() {
        KakaoLoginActivityProvider.detach(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadKoinModules(adminModule)
        setContent {
            RamapTheme {
                AdminLoginRoute(onBack = ::finish)
            }
        }
    }
}
