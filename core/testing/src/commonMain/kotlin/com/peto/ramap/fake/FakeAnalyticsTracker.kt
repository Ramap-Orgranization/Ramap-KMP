package com.peto.ramap.fake

import com.peto.ramap.analytics.AnalyticsTracker

class FakeAnalyticsTracker : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val screenViews = mutableListOf<Pair<String, Map<String, Any>>>()
    val userProperties = mutableMapOf<String, String>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>,
    ) {
        events += name to params
    }

    override fun logScreenView(
        screenName: String,
        params: Map<String, Any>,
    ) {
        screenViews += screenName to params
    }

    override fun setUserProperty(
        key: String,
        value: String,
    ) {
        userProperties[key] = value
    }
}
