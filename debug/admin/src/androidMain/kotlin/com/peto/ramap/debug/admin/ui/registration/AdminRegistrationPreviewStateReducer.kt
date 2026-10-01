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
        val resolvedEventType = resolveEventType(preview.eventType, state.selectedEventType)
        val resolvedEndDate =
            if (resolvedEventType == ShopEventType.STORE_RENEWAL) {
                null
            } else {
                state.selectedEndDate ?: preview.endDate
            }

        return state.copy(
            draft =
                preview.copy(
                    shopName = preview.shopName ?: state.shopName.ifBlank { null },
                    sourceUrl = preview.sourceUrl ?: state.sourceUrl.ifBlank { null },
                    startDate = state.selectedStartDate ?: preview.startDate,
                    endDate = resolvedEndDate,
                    noticeType = state.selectedNoticeType?.let(::noticeTypeRequestValue) ?: preview.noticeType,
                ),
            selectedEventType = resolvedEventType,
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
