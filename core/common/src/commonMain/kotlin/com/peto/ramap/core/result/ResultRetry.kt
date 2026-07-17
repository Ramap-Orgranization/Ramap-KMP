package com.peto.ramap.core.result

suspend inline fun <T> retryOnce(block: () -> RamapResult<T>): RamapResult<T> {
    val first = block()
    return if (first is RamapResult.Error) block() else first
}
