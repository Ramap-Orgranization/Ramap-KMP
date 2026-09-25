package com.peto.ramap.ui.report.contract

import com.peto.ramap.ui.base.State
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class PlaceReportUiState(
    val placeUrl: String = "",
    override val loadState: LoadState = LoadState(),
) : State,
    LoadableState<PlaceReportUiState> {
    /** URL 제보 저장이 진행 중인지 여부. */
    val isSubmitting: Boolean
        get() = loadState.isLoading(PlaceReportLoadKey.Submit)

    val canSubmitPlaceUrl: Boolean
        get() = placeUrl.isNotBlank() && !isSubmitting

    override fun withLoadingState(loadState: LoadState): PlaceReportUiState = copy(loadState = loadState)
}
