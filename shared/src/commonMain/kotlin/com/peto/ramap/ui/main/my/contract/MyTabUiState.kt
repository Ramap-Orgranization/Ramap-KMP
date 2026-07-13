package com.peto.ramap.ui.main.my.contract

import com.peto.ramap.core.base.State
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.PlaceReportTextParser

data class MyTabUiState(
    val isLoggedIn: Boolean = false,
    val accountLabel: String? = null,
    val isDeletingAccount: Boolean = false,
    val currentLocation: Location? = null,
    val currentAddress: String? = null,
    val placeUrl: String = "",
    val showAccountDeleteConfirmDialog: Boolean = false,
    val showLoginGuideDialog: Boolean = false,
) : State {
    val canSubmitPlaceUrl: Boolean
        get() = PlaceReportTextParser.extractSupportedUrl(placeUrl) != null
}
