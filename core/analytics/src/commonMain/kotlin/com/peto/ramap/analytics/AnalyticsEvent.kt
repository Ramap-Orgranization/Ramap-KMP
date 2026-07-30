package com.peto.ramap.analytics

interface AnalyticsEvent {
    val name: String

    fun params(): Map<String, Any> = emptyMap()
}
