package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.ui.base.Intent

sealed interface OperatingNoticeIntent : Intent {
    data object OnRefreshed : OperatingNoticeIntent

    data object OnRetried : OperatingNoticeIntent
}
