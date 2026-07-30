package com.peto.ramap.fake

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<AnalyticsEvent>()
    val screenViews = mutableListOf<Pair<String, Map<String, Any>>>()
    val userProperties = mutableMapOf<String, String>()

    override fun logEvent(event: AnalyticsEvent) {
        events += event
    }

    override fun logScreenView(
        screenName: String,
        params: Map<String, Any>,
    ) {
        screenViews += screenName to params
    }

    override fun userProperty(
        key: String,
        value: String,
    ) {
        userProperties[key] = value
    }
}
