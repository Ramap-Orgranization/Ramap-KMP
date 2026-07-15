package com.peto.ramap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.peto.ramap.data.auth.KakaoLoginActivityProvider
import com.peto.ramap.notification.DeepLinkKey
import com.peto.ramap.notification.NotificationLaunchDispatcher
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val notificationLaunchDispatcher: NotificationLaunchDispatcher by inject()

    override fun onResume() {
        super.onResume()
        KakaoLoginActivityProvider.attach(this)
        ExternalUriOpener.attach(this)
        NotificationPermissionRequester.attach(this)
    }

    override fun onPause() {
        KakaoLoginActivityProvider.detach(this)
        ExternalUriOpener.detach(this)
        NotificationPermissionRequester.detach(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleAuthDeepLink(intent)
        handleNotificationDeepLink(intent)

        setContent {
            App(onExitRequested = ::finish)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
        handleNotificationDeepLink(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermissionRequester.complete(requestCode, grantResults)
    }

    private fun handleNotificationDeepLink(intent: Intent) {
        notificationLaunchDispatcher.dispatch(intent.getStringExtra(DeepLinkKey.NOTIFICATION_DEEP_LINK_KEY))
    }
}
