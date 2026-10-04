package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationUiState
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType

internal object AdminRegistrationPreviewStateReducer {
    fun reduce(
        state: AdminRegistrationUiState,
        preview: AdminDraft,
    ): AdminRegistrationUiState {
        val isMonthly = state.isOperatingNotice && preview.dailySchedules.isNotEmpty()
        val resolvedEventType = resolveEventType(preview.eventType, state.selectedEventType)
        val resolvedEndDate =
            if (!state.isOperatingNotice && resolvedEventType == ShopEventType.STORE_RENEWAL) {
                null
            } else {
                if (isMonthly) preview.endDate else state.selectedEndDate ?: preview.endDate
            }

        return state.copy(
            draft =
                preview.copy(
                    shopName = preview.shopName ?: state.shopName.ifBlank { null },
                    sourceUrl = preview.sourceUrl ?: state.sourceUrl.ifBlank { null },
                    startDate = if (isMonthly) preview.startDate else state.selectedStartDate ?: preview.startDate,
                    endDate = resolvedEndDate,
                    noticeType = if (isMonthly) "operating_notice" else state.selectedNoticeType?.let(::noticeTypeRequestValue) ?: preview.noticeType,
                ),
            selectedEventType = resolvedEventType,
            selectedNoticeType = if (isMonthly) OperatingNoticeType.OPERATING_NOTICE else state.selectedNoticeType,
            selectedStartDate = if (isMonthly) preview.startDate else state.selectedStartDate,
            selectedEndDate = if (isMonthly) preview.endDate else state.selectedEndDate,
            message = null,
        )
    }

    private fun resolveEventType(
        previewEventType: String?,
        selectedEventType: ShopEventType,
    ): ShopEventType =
        previewEventType
            ?.let { eventType -> runCatching { ShopEventType.from(eventType) }.getOrNull() }
            ?: selectedEventType

    private fun noticeTypeRequestValue(noticeType: OperatingNoticeType): String =
        when (noticeType) {
            OperatingNoticeType.OPERATING_NOTICE -> "operating_notice"
            OperatingNoticeType.TEMPORARY_CLOSURE -> "full_close"
            OperatingNoticeType.EARLY_CLOSING -> "early_close"
            OperatingNoticeType.LATE_OPENING -> "late_opening"
        }
}
