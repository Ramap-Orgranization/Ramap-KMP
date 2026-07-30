package com.peto.ramap.ui.report.contract

import com.peto.ramap.platform.permission.PermissionStatus
import com.peto.ramap.ui.base.Intent

sealed interface PlaceReportIntent : Intent {
    data class OnPlaceUrlChanged(
        val value: String,
    ) : PlaceReportIntent

    data object OnPlaceReportSubmit : PlaceReportIntent

    data object OnCurrentLocationReportSubmit : PlaceReportIntent

    data class OnLocationPermissionResult(
        val status: PermissionStatus,
    ) : PlaceReportIntent
}
