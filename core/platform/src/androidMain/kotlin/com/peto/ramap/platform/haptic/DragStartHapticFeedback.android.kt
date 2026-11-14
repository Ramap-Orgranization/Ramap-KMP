package com.peto.ramap.platform.haptic

import android.content.ContentResolver
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import io.github.compose.jindong.core.executor.createHapticExecutor

@Composable
actual fun rememberDragStartHapticFeedback(): () -> Unit {
    val view = LocalView.current
    val executor =
        remember(view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) createHapticExecutor(view.context.applicationContext) else null
        }
    return remember(view, executor) {
        {
            if (executor != null && view.isHapticFeedbackEnabled && touchHapticsEnabled(view.context.contentResolver)) {
                executor.executeAsync(dragStartHapticPattern)
            }
        }
    }
}

// Jindong 1.1.0 plays without USAGE_TOUCH, so check the system's touch feedback switch first.
@Suppress("DEPRECATION")
private fun touchHapticsEnabled(contentResolver: ContentResolver): Boolean = Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
