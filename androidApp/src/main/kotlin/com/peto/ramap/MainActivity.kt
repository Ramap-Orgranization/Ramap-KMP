package com.peto.ramap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.peto.ramap.data.auth.KakaoLoginActivityProvider

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onResume() {
        super.onResume()
        KakaoLoginActivityProvider.attach(this)
    }

    override fun onPause() {
        KakaoLoginActivityProvider.detach(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthDeepLink(intent)
        requestNotificationPermission()

        setContent {
            App(onExitRequested = ::finish)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
    private fun handleNotificationDeepLink(intent: Intent) {
        notificationLaunchDispatcher.dispatch(intent.getStringExtra(DeepLinkKey.NOTIFICATION_DEEP_LINK_KEY))
    }
}
