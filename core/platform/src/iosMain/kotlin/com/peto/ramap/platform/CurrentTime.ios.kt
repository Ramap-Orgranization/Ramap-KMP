package com.peto.ramap.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual fun currentEpochMillis(): Long = time(null) * 1_000L
