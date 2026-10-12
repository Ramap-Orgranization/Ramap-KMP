package com.peto.ramap.debug.admin

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.peto.ramap.debug.admin.di.adminModule
import com.peto.ramap.debug.admin.ui.login.AdminLoginRoute
import com.peto.ramap.theme.RamapTheme
import org.koin.core.context.loadKoinModules

class AdminRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        loadKoinModules(adminModule)
        setContent {
            RamapTheme {
                AdminLoginRoute(onBack = ::finish)
            }
        }
    }
}
