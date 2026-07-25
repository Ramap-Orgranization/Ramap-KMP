@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.peto.ramap.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import platform.Foundation.NSNumber
import platform.Foundation.numberWithBool
import platform.Foundation.numberWithDouble
import platform.Foundation.numberWithInt
import platform.Foundation.numberWithLong

class FirebaseAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(event: AnalyticsEvent) {
        FIRAnalytics.logEventWithName(event.name, event.params().toNsParams())
    }

    override fun logScreenView(
        screenName: String,
        params: Map<String, Any>,
    ) {
        val allParams = params + ("screen_name" to screenName)
        FIRAnalytics.logEventWithName("screen_view", allParams.toNsParams())
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
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
