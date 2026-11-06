package com.peto.ramap.designsystem.image

import androidx.compose.runtime.staticCompositionLocalOf
import com.peto.ramap.domain.model.image.ImageAccessScope

val LocalImageAccessScope = staticCompositionLocalOf { ImageAccessScope(null, 0, 0) }
