@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.peto.ramap.analytics

import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseCrashlytics.FIRCrashlytics
import platform.Foundation.NSError

class FirebaseCrashReporter : CrashReporter {
    private val isConfigured: Boolean
        get() = FIRApp.defaultApp() != null

    private val crashlytics: FIRCrashlytics
        get() = FIRCrashlytics.crashlytics()

    override fun log(message: String) {
        if (!isConfigured) return
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        if (!isConfigured) return
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
        if (!isConfigured) return
        crashlytics.setCustomValue(value, key)
    }
}
