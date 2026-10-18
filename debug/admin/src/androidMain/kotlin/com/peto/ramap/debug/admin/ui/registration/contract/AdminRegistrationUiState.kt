package com.peto.ramap.debug.admin.ui.registration.contract

import com.peto.ramap.debug.admin.data.model.AdminCorrectionPreview
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.data.model.AdminEvidence
import com.peto.ramap.debug.admin.data.model.AdminExternalVenue
import com.peto.ramap.debug.admin.data.model.AdminManagedEvent
import com.peto.ramap.debug.admin.data.model.AdminShopHours
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import java.time.LocalDate
import java.time.ZoneId

internal data class AdminRegistrationUiState(
    val shopNames: List<String> = emptyList(),
    val externalVenues: List<AdminExternalVenue> = emptyList(),
    val isOperatingNotice: Boolean = false,
    val selectedNoticeType: OperatingNoticeType? = null,
    val hasSelectedEventType: Boolean = false,
    val selectedEventType: ShopEventType = ShopEventType.LIMITED_MENU,
    val shopName: String = "",
    val selectedShopHours: AdminShopHours? = null,
    val sourceUrl: String = "",
    val feedback: String = "",
    val isImageOnly: Boolean = false,
    val imageOnlyTitle: String = "",
    val selectedStartDate: String? = null,
    val selectedEndDate: String? = null,
    val evidence: AdminEvidence? = null,
    val draft: AdminDraft? = null,
    val message: AdminRegistrationMessage? = null,
    val managedEvents: List<AdminManagedEvent> = emptyList(),
    val selectedManagedEventId: String? = null,
    val selectedEventStatus: AdminEventStatus = AdminEventStatus.SOLD_OUT,
    val selectedEventStatusScope: AdminEventStatusScope = AdminEventStatusScope.TODAY,
    val eventStatusReason: String = "",
    val eventStatusStartDate: String? = null,
    val eventStatusEndDate: String? = null,
    val editingEventId: String? = null,
    val correctionRequest: String = "",
    val correctionPreview: AdminCorrectionPreview? = null,
    val selectedTab: AdminRegistrationTab = AdminRegistrationTab.EVENT_MANAGEMENT,
    val today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul")),
    val managedEventsLoadFailed: Boolean = false,
    override val loadState: LoadState = LoadState(),
) : LoadableState<AdminRegistrationUiState> {
    val isSubmitting: Boolean get() = loadState.isLoading(AdminRegistrationLoadKey.Submission)
    val isSavingEventStatus: Boolean get() = loadState.isLoading(AdminRegistrationLoadKey.EventStatus)
    val isCorrecting: Boolean get() = loadState.isLoading(AdminRegistrationLoadKey.Correction)
    val isLoadingEvents: Boolean get() = loadState.isLoading(AdminRegistrationLoadKey.ManagedEvents)
    val isBusy: Boolean get() = isSubmitting || isSavingEventStatus || isCorrecting

    fun restoreForm(form: AdminRegistrationUiState): AdminRegistrationUiState =
        copy(
            selectedNoticeType = form.selectedNoticeType,
            selectedEventType = form.selectedEventType,
            hasSelectedEventType = form.hasSelectedEventType,
            shopName = form.shopName,
            selectedShopHours = form.selectedShopHours,
            sourceUrl = form.sourceUrl,
            feedback = form.feedback,
            isImageOnly = form.isImageOnly,
            imageOnlyTitle = form.imageOnlyTitle,
            selectedStartDate = form.selectedStartDate,
            selectedEndDate = form.selectedEndDate,
            evidence = form.evidence,
            draft = form.draft,
        )

    override fun withLoadingState(loadState: LoadState): AdminRegistrationUiState = copy(loadState = loadState)
}
