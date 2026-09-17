package com.peto.ramap.ui.main.notice.log.event

import com.peto.ramap.analytics.AnalyticsEvent

internal data class OperatingNoticeSelected(
    val noticeId: String,
) : AnalyticsEvent {
    override val name: String = "operating_notice_select"

    override fun params(): Map<String, Any> =
        mapOf(
            "content_type" to "operating_notice",
            "operating_notice_id" to noticeId,
        )
}
