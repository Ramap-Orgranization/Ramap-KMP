package com.peto.ramap.ui.report.contract

import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.common.LoadState

data class PlaceReportUiState(
    val currentLocation: Location? = null,
    val currentAddress: String? = null,
    val isLocationLoading: Boolean = false,
    val isAddressRefreshing: Boolean = false,
    val placeUrl: String = "",
    val submitState: LoadState<Unit> = LoadState.Idle,
) : State {
    val isSubmitting: Boolean
        get() = submitState == LoadState.Loading

    val canSubmitPlaceUrl: Boolean
        get() = placeUrl.isNotBlank() && !isSubmitting

    val canSubmitCurrentLocation: Boolean
        get() = currentLocation != null && !isSubmitting && !isLocationLoading
}
