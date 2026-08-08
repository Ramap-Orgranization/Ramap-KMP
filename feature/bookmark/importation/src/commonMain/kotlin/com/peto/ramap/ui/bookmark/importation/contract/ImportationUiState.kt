package com.peto.ramap.ui.bookmark.importation.contract

import com.peto.ramap.domain.model.importation.ImportationPreview
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class ImportationUiState(
    val url: String = "",
    val preview: ImportationPreview? = null,
    val candidates: RamenShops = RamenShops(emptyMap()),
    val alreadyBookmarkedCount: Int = 0,
    val hiddenCount: Int = 0,
    val error: ImportationError? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ImportationUiState> {
    override fun withLoadingState(loadState: LoadState): ImportationUiState = copy(loadState = loadState)

    val isAnalyzing: Boolean = loadState.isLoading(ImportationLoadKey.ANALYZE)
    val isConfirming: Boolean = loadState.isLoading(ImportationLoadKey.CONFIRM)
    val isBusy: Boolean = isAnalyzing || isConfirming
}
