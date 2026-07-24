@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.peto.ramap.analytics

import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import platform.Foundation.NSError

class FirebaseCrashReporter : CrashReporter {
    private val crashlytics: FIRCrashlytics
        get() = FIRCrashlytics.crashlytics()

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        val error =
            NSError(
                domain = "com.peto.ramap",
                code = 0,
                userInfo = mapOf<Any?, Any?>("message" to (throwable.message ?: throwable.toString())),
            )
        crashlytics.recordError(error)
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        crashlytics.setCustomValue(value, key)
    }
}
