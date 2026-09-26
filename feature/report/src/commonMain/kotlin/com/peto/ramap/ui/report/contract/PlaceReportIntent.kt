package com.peto.ramap.ui.report.contract

import com.peto.ramap.ui.base.Intent

sealed interface PlaceReportIntent : Intent {
    data class OnPlaceUrlChanged(
        val value: String,
    ) : PlaceReportIntent

    data object OnPlaceReportSubmit : PlaceReportIntent
}
