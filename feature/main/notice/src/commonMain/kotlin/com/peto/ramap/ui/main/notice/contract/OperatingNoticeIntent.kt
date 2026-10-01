package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.ui.base.Intent

sealed interface OperatingNoticeIntent : Intent {
    data class OnNoticeClicked(
        val notice: OperatingNotice,
    ) : OperatingNoticeIntent

    data object OnRefreshed : OperatingNoticeIntent

    data object OnRetried : OperatingNoticeIntent
}
