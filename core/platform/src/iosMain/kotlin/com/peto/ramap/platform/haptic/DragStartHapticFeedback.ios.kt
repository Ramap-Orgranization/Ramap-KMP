package com.peto.ramap.platform.haptic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import io.github.compose.jindong.core.executor.createHapticExecutor

@Composable
actual fun rememberDragStartHapticFeedback(): () -> Unit {
    val executor = remember { createHapticExecutor() }
    val systemFeedback = LocalHapticFeedback.current
    DisposableEffect(executor) {
        onDispose { executor.release() }
    }
    return remember(executor, systemFeedback) {
        {
            if (!executor.executeAsync(dragStartHapticPattern).isActive) {
                systemFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }
}
