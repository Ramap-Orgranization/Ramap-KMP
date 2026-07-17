package com.peto.ramap.core.result

sealed interface RamapResult<out T> {
    data class Success<T>(
        val data: T,
    ) : RamapResult<T>

    data class Error(
        val error: RamapError,
    ) : RamapResult<Nothing>
}

fun <T> RamapResult<T>.getOrThrow(): T =
    when (this) {
        is RamapResult.Success -> data
        is RamapResult.Error -> throw error.cause ?: IllegalStateException(error.toString())
    }
