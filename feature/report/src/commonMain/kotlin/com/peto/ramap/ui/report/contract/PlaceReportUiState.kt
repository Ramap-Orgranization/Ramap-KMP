package com.peto.ramap.ui.report.contract

import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class PlaceReportUiState(
    val currentLocation: Location? = null,
    val currentAddress: String? = null,
    val placeUrl: String = "",
    override val loadState: LoadState = LoadState(),
) : State,
    LoadableState<PlaceReportUiState> {
    /** 플랫폼 현재 위치 요청이 진행 중인지 여부. */
    val isLocationLoading: Boolean
        get() = loadState.isLoading(PlaceReportLoadKey.CurrentLocation)

    /** 현재 위치의 주소 변환이 진행 중인지 여부. */
    val isAddressRefreshing: Boolean
        get() = loadState.isLoading(PlaceReportLoadKey.Address)

    /** URL 또는 현재 위치 제보 저장이 진행 중인지 여부. */
    val isSubmitting: Boolean
        get() = loadState.isLoading(PlaceReportLoadKey.Submit)

    val canSubmitPlaceUrl: Boolean
        get() = placeUrl.isNotBlank() && !isSubmitting

    val canSubmitCurrentLocation: Boolean
        get() = currentLocation != null && !isSubmitting && !isLocationLoading

    override fun withLoadingState(loadState: LoadState): PlaceReportUiState = copy(loadState = loadState)
}
