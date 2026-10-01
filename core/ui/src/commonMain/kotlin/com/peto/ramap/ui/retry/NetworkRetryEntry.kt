package com.peto.ramap.ui.retry

internal class NetworkRetryEntry(
    val delayMillis: Long,
    val retry: suspend () -> Unit,
)
