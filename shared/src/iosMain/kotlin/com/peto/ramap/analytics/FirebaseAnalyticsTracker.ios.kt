@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.peto.ramap.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import cocoapods.FirebaseCore.FIRApp
import platform.Foundation.NSNumber
import platform.Foundation.numberWithBool
import platform.Foundation.numberWithDouble
import platform.Foundation.numberWithInt
import platform.Foundation.numberWithLong

class FirebaseAnalyticsTracker : AnalyticsTracker {
    private val isConfigured: Boolean
        get() = FIRApp.defaultApp() != null

    override fun logEvent(event: AnalyticsEvent) {
        if (!isConfigured) return
        FIRAnalytics.logEventWithName(event.name, event.params().toNsParams())
    }

    override fun logScreenView(
        screenName: String,
        params: Map<String, Any>,
    ) {
        if (!isConfigured) return
        val allParams = params + ("screen_name" to screenName)
        FIRAnalytics.logEventWithName("screen_view", allParams.toNsParams())
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        if (!isConfigured) return
        FIRAnalytics.setUserPropertyString(value, key)
    }

    private fun Map<String, Any>.toNsParams(): Map<Any?, *> =
        mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Int -> NSNumber.numberWithInt(value)
                is Long -> NSNumber.numberWithLong(value)
                is Double -> NSNumber.numberWithDouble(value)
                is Boolean -> NSNumber.numberWithBool(value)
                else -> value.toString()
            }
        }
}
