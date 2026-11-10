package com.peto.ramap.ui.onboarding.shopselection.log

import com.peto.ramap.core.result.RamapError

public interface ShopSelectionAnalytics {
    fun shopLoadResult(
        operation: String,
        resultCount: Int,
    )

    fun shopLoadFailed(
        operation: String,
        error: RamapError,
    )

    fun bookmarkResult(
        enabled: Boolean,
        savedCount: Int,
        error: RamapError? = null,
    )

    fun finishRequested(savedCount: Int)

    fun finishFailed(savedCount: Int)

    fun completed(savedCount: Int)
}
