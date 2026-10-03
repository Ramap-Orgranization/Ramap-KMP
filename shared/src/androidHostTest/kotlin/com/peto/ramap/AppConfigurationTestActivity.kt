package com.peto.ramap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.peto.ramap.theme.RamapTheme

class AppConfigurationTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RamapTheme { content() } }
    }

    companion object {
        var content: @Composable () -> Unit = {}
    }
}
