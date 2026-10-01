package com.peto.ramap.ui.main.notice.log

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.ui.main.notice.log.event.OperatingNoticeSelected

class OperatingNoticeAnalytics(
    private val analyticsTracker: AnalyticsTracker,
) {
    fun logNoticeSelected(notice: OperatingNotice) {
        analyticsTracker.logEvent(OperatingNoticeSelected(notice.id))
    }
}
