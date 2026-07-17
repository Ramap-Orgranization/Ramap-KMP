package com.peto.ramap.ui.report.contract

import com.peto.ramap.domain.model.report.PlaceReportTextParser
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.ui.base.State

data class PlaceReportUiState(
    val currentLocation: Location? = null,
    val currentAddress: String? = null,
    val isAddressRefreshing: Boolean = false,
    val placeUrl: String = "",
) : State {
    val canSubmitPlaceUrl: Boolean
        get() = PlaceReportTextParser.extractSupportedUrl(placeUrl) != null
}
