package com.peto.ramap.ui.main.notice.contract

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.ui.base.Intent

sealed interface OperatingNoticeIntent : Intent {
    data class OnNoticeClicked(
        val notice: OperatingNotice,
    ) : OperatingNoticeIntent

    data class OnTabSelected(
        val scheduled: Boolean,
    ) : OperatingNoticeIntent

    data class OnQueryChanged(
        val query: String,
    ) : OperatingNoticeIntent

    data object OnSavedToggled : OperatingNoticeIntent

    data object OnPersonalizationRetried : OperatingNoticeIntent

    data object OnRefreshed : OperatingNoticeIntent

    data object OnRetried : OperatingNoticeIntent
}
