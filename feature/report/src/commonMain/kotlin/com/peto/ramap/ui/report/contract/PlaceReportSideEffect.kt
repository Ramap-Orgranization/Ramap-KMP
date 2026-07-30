package com.peto.ramap.ui.report.contract

import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.ui.base.SideEffect

sealed interface PlaceReportSideEffect : SideEffect {
    data class ShowToast(
        val data: ToastData,
    ) : PlaceReportSideEffect
}
