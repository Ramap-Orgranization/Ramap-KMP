package com.peto.ramap

import co.touchlab.kermit.Logger
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook

private val unhandledExceptionLogger = Logger.withTag("RamapUnhandledException")

@OptIn(ExperimentalNativeApi::class)
fun installUnhandledExceptionLogger() {
    setUnhandledExceptionHook { throwable ->
        unhandledExceptionLogger.e(throwable) {
            "Unhandled Kotlin exception: ${throwable::class.simpleName}: ${throwable.message}"
        }
        throwable.stackTraceToString().lines().forEach { line ->
            unhandledExceptionLogger.e { line }
        }
    }
}
