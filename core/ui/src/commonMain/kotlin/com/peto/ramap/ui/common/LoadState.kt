package com.peto.ramap.ui.common

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>

    data object Loading : LoadState<Nothing>

    data class Content<T>(
        val data: T,
    ) : LoadState<T>

    data object Error : LoadState<Nothing>
}
