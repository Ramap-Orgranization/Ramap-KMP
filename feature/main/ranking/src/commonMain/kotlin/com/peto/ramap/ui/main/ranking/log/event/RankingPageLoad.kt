package com.peto.ramap.ui.main.ranking.log.event

import com.peto.ramap.analytics.AnalyticsEvent
import com.peto.ramap.analytics.AnalyticsEvents

internal data object RankingPageLoad : AnalyticsEvent {
    override val name: String = AnalyticsEvents.RANKING_PAGE_LOAD
}
