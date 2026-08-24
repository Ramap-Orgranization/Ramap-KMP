package com.peto.ramap.debug.admin.ui.registration.contract

import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.ui.base.Intent

internal sealed interface AdminRegistrationIntent : Intent {
    data class OnRegistrationTypeSelected(
        val isOperatingNotice: Boolean,
    ) : AdminRegistrationIntent

    data class OnNoticeTypeSelected(
        val noticeType: OperatingNoticeType,
    ) : AdminRegistrationIntent

    data class OnEventTypeSelected(
        val eventType: ShopEventType,
    ) : AdminRegistrationIntent

    data class OnShopNameChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnSourceUrlChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnFeedbackChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnDraftTitleChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnDraftDescriptionChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnEvidenceSelected(
        val evidence: AdminEvidence?,
    ) : AdminRegistrationIntent

    data class OnDateRangeSelected(
        val startDate: String,
        val endDate: String,
    ) : AdminRegistrationIntent

    data object OnTodaySelected : AdminRegistrationIntent

    data object OnPreviewOrRegisterClicked : AdminRegistrationIntent

    data object OnManagedEventsRefreshed : AdminRegistrationIntent

    data class OnManagedEventSelected(
        val eventId: String,
    ) : AdminRegistrationIntent

    data class OnEventStatusSelected(
        val status: AdminEventStatus,
    ) : AdminRegistrationIntent

    data class OnEventStatusScopeSelected(
        val scope: AdminEventStatusScope,
    ) : AdminRegistrationIntent

    data class OnEventStatusReasonChanged(
        val value: String,
    ) : AdminRegistrationIntent

    data class OnEventStatusDateRangeSelected(
        val startDate: String,
        val endDate: String,
    ) : AdminRegistrationIntent

    data object OnEventStatusTodaySelected : AdminRegistrationIntent

    data object OnEventStatusSaved : AdminRegistrationIntent

    data class OnTabSelected(
        val tab: AdminRegistrationTab,
    ) : AdminRegistrationIntent
}
