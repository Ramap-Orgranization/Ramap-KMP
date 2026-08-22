package com.peto.ramap.ui.main.event.list.contract

import com.peto.ramap.domain.model.event.EventFilter
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.report.NewsReportEvidence
import com.peto.ramap.ui.base.Intent

sealed interface EventsIntent : Intent {
    data object OnEventsRefreshed : EventsIntent

    data object OnEventsRetried : EventsIntent

    data class OnFilterSelected(
        val filter: EventFilter,
    ) : EventsIntent

    data class OnEventClicked(
        val event: ShopEvent,
    ) : EventsIntent

    data object OnNewsReportClicked : EventsIntent

    data object OnNewsReportDismissed : EventsIntent

    data class OnNewsReportContentChanged(
        val value: String,
    ) : EventsIntent

    data class OnNewsReportEvidenceSelected(
        val evidence: NewsReportEvidence,
    ) : EventsIntent

    data object OnNewsReportEvidenceRemoved : EventsIntent

    data object OnNewsReportSubmit : EventsIntent
}
