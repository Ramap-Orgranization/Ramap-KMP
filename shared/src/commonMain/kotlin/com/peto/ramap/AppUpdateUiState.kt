package com.peto.ramap

import com.peto.ramap.domain.model.update.AppUpdatePolicy
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

internal data class AppUpdateUiState(
    val policy: AppUpdatePolicy? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<AppUpdateUiState> {
    override fun withLoadingState(loadState: LoadState): AppUpdateUiState = copy(loadState = loadState)
}
