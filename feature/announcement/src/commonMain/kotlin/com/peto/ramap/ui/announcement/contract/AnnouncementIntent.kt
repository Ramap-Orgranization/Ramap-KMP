package com.peto.ramap.ui.announcement.contract

import com.peto.ramap.ui.base.Intent

sealed interface AnnouncementIntent : Intent {
    data object Refresh : AnnouncementIntent

    data object ReturnToScreen : AnnouncementIntent

    data object Retry : AnnouncementIntent

    data class Open(
        val id: String,
    ) : AnnouncementIntent

    data object Dismiss : AnnouncementIntent
}
