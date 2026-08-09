package com.peto.ramap.ui.retry

object NetworkRetryGenerator {
    private val pendingRetries = mutableMapOf<Any, MutableMap<String, suspend () -> Unit>>()

    fun enqueue(
        owner: Any,
        taskKey: String,
        retry: suspend () -> Unit,
    ) {
        pendingRetries.getOrPut(owner) { mutableMapOf() }[taskKey] = retry
    }

    fun remove(
        owner: Any,
        taskKey: String,
    ) {
        val retries = pendingRetries[owner] ?: return
        retries.remove(taskKey)
        if (retries.isEmpty()) pendingRetries.remove(owner)
    }

    fun remove(owner: Any) {
        pendingRetries.remove(owner)
    }

    fun clear() {
        pendingRetries.clear()
    }

    suspend fun retryPending() {
        val retries = pendingRetries.values.flatMap { it.values }
        pendingRetries.clear()
        for (retry in retries) retry()
    }
}
