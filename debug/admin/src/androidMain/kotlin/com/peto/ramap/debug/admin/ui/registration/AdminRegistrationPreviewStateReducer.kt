package com.peto.ramap.debug.admin.ui.registration

import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationUiState
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.notice.OperatingNoticeType

internal object AdminRegistrationPreviewStateReducer {
    fun reduce(
        state: AdminRegistrationUiState,
        result: AdminDraft,
    ): AdminRegistrationUiState {
        val singleClosure = result.dailySchedules.singleOrNull()?.takeIf { it.closed && it.scheduleOverride == null }
        val preview =
            if (state.isOperatingNotice && singleClosure != null) {
                result.copy(
                    startDate = singleClosure.date,
                    endDate = singleClosure.date,
                    noticeType = "full_close",
                    dailySchedules = emptyList(),
                )
            } else {
                result
            }
        val isMonthly = state.isOperatingNotice && preview.dailySchedules.isNotEmpty()
        val resolvedNoticeType = if (isMonthly) "full_close" else state.selectedNoticeType?.let(::noticeTypeRequestValue) ?: preview.noticeType
        val resolvedEventType = if (state.hasSelectedEventType) state.selectedEventType else resolveEventType(preview.eventType, state.selectedEventType)
        val resolvedEndDate =
            if (!state.isOperatingNotice && resolvedEventType == ShopEventType.STORE_RENEWAL) {
                null
            } else if (!state.isOperatingNotice && resolvedEventType == ShopEventType.NEW_MENU) {
                (state.selectedStartDate ?: preview.startDate)?.let {
                    java.time.LocalDate
                        .parse(it)
                        .plusDays(6)
                        .toString()
                }
            } else {
                if (isMonthly) preview.endDate else state.selectedEndDate ?: preview.endDate
            }

        return state.copy(
            shopName = preview.shopName ?: state.shopName,
            draft =
                preview.copy(
                    shopName = preview.shopName ?: state.shopName.ifBlank { null },
                    sourceUrl = preview.sourceUrl ?: state.sourceUrl.ifBlank { null },
                    startDate = if (isMonthly) preview.startDate else state.selectedStartDate ?: preview.startDate,
                    endDate = resolvedEndDate,
                    noticeType = resolvedNoticeType,
                    startTime = if (state.isOperatingNotice) null else preview.startTime,
                    endTime = if (state.isOperatingNotice && resolvedNoticeType != "early_close") null else preview.endTime,
                    scheduleOverride = if (state.isOperatingNotice) null else preview.scheduleOverride,
                ),
            selectedEventType = resolvedEventType,
            selectedNoticeType = if (isMonthly) OperatingNoticeType.TEMPORARY_CLOSURE else state.selectedNoticeType,
            selectedStartDate = if (isMonthly) preview.startDate else state.selectedStartDate ?: preview.startDate,
            selectedEndDate = resolvedEndDate,
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
            OperatingNoticeType.TEMPORARY_CLOSURE -> "full_close"
            OperatingNoticeType.EARLY_CLOSING -> "early_close"
        }
}
