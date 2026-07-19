package com.peto.ramap.ui.main.map.viewport

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.domain.model.shop.RamenShops

sealed interface ViewportLoadResult {
    data class Loaded(
        val shops: RamenShops,
    ) : ViewportLoadResult

    data class Failed(
        val error: RamapError,
    ) : ViewportLoadResult
}
