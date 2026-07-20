package com.peto.ramap.ui.base

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState

data class TestState(
    val results: List<String> = emptyList(),
    override val loadState: LoadState = LoadState(),
) : LoadableState<TestState> {
    override fun withLoadingState(loadState: LoadState): TestState = copy(loadState = loadState)
}
