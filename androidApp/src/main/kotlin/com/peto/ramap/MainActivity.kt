package com.peto.ramap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.peto.ramap.data.auth.KakaoLoginActivityProvider

class MainActivity : ComponentActivity() {
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

        setContent {
            App(onExitRequested = ::finish)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthDeepLink(intent)
    }
}
