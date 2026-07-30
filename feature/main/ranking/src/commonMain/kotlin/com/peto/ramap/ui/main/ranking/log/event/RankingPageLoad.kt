package com.peto.ramap.ui.main.ranking.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data object RankingPageLoad : AnalyticsEvent {
    override val name: String = "ranking_page_load"
}
