package com.peto.ramap.platform.haptic

import androidx.compose.runtime.Composable
import io.github.compose.jindong.core.dsl.buildHapticPattern
import io.github.compose.jindong.core.model.HapticIntensity
import kotlin.time.Duration.Companion.milliseconds

@Composable
expect fun rememberDragStartHapticFeedback(): () -> Unit

internal val dragStartHapticPattern =
    buildHapticPattern {
        haptic(duration = 45.milliseconds, intensity = HapticIntensity.STRONG)
    }
