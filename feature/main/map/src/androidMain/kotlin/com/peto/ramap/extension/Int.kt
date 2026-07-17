package com.peto.ramap.extension

import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

fun Int.toPx(density: Density): Int = (this * density.density).roundToInt()
