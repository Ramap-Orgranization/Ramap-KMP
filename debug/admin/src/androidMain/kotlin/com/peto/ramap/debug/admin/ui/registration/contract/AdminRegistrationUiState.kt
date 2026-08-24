package com.peto.ramap.debug.admin.ui.registration.contract

import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.ui.base.State

internal data class AdminRegistrationUiState(
    val shopNames: List<String> = emptyList(),
    val isOperatingNotice: Boolean = false,
    val selectedNoticeType: OperatingNoticeType? = null,
    val selectedEventType: ShopEventType = ShopEventType.LIMITED_MENU,
    val shopName: String = "",
    val sourceUrl: String = "",
    val feedback: String = "",
    val isImageOnly: Boolean = false,
    val imageOnlyTitle: String = "",
    val selectedStartDate: String? = null,
    val selectedEndDate: String? = null,
    val evidence: AdminEvidence? = null,
    val draft: AdminDraft? = null,
    val message: AdminRegistrationMessage? = null,
    val isSubmitting: Boolean = false,
    val managedEvents: List<AdminManagedEvent> = emptyList(),
    val selectedManagedEventId: String? = null,
    val selectedEventStatus: AdminEventStatus = AdminEventStatus.SOLD_OUT,
    val selectedEventStatusScope: AdminEventStatusScope = AdminEventStatusScope.TODAY,
    val eventStatusReason: String = "",
    val eventStatusStartDate: String? = null,
    val eventStatusEndDate: String? = null,
    val isSavingEventStatus: Boolean = false,
    val selectedTab: AdminRegistrationTab = AdminRegistrationTab.EVENT_MANAGEMENT,
) : State
