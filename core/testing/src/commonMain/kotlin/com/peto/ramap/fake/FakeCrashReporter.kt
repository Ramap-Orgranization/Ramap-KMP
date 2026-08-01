package com.peto.ramap.fake

import com.peto.ramap.analytics.CrashReporter

class FakeCrashReporter : CrashReporter {
    val messages = mutableListOf<String>()
    val exceptions = mutableListOf<Throwable>()
    val customKeys = mutableMapOf<String, String>()

    override fun log(message: String) {
        messages += message
    }

    override fun recordException(throwable: Throwable) {
        exceptions += throwable
    }

    override fun setCustomKey(
        key: String,
        value: String,
    ) {
        customKeys[key] = value
    }
}
