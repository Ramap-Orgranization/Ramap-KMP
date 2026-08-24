package com.peto.ramap.debug.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.peto.ramap.debug.admin.di.adminModule
import com.peto.ramap.debug.admin.ui.registration.AdminRegistrationRoute
import com.peto.ramap.theme.RamapTheme
import org.koin.core.context.loadKoinModules

class AdminRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadKoinModules(adminModule)
        setContent {
            RamapTheme {
                AdminRegistrationRoute()
            }
        }
    }
}
