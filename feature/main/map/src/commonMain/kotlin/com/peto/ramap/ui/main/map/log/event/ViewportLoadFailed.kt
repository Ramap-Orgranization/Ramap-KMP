package com.peto.ramap.ui.main.map.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data object ViewportLoadFailed : AnalyticsEvent {
    override val name: String = "viewport_load_error"
}
