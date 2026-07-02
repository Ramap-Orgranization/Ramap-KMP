package com.peto.ramap.ui.extension

fun Int.alphaComponent(): Double = ((this shr 24) and 0xFF) / 255.0

fun Int.redComponent(): Double = ((this shr 16) and 0xFF) / 255.0

fun Int.greenComponent(): Double = ((this shr 8) and 0xFF) / 255.0

fun Int.blueComponent(): Double = (this and 0xFF) / 255.0
