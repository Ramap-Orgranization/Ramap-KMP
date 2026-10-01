package com.peto.ramap.ui.retry

import kotlinx.coroutines.delay
import kotlin.random.Random

object NetworkRetryGenerator {
    private val pendingRetries = mutableMapOf<Any, MutableMap<String, NetworkRetryEntry>>()
    private val runningRetries = mutableSetOf<NetworkRetryEntry>()

    fun enqueue(
        owner: Any,
        taskKey: String,
        retryAttempt: Int = 1,
        retry: suspend () -> Unit,
    ) {
        if (retryAttempt !in 1..MAX_RETRY_ATTEMPTS) return
        val delayMillis = INITIAL_DELAY_MILLIS * (1L shl (retryAttempt - 1))
        pendingRetries.getOrPut(owner) { mutableMapOf() }[taskKey] =
            NetworkRetryEntry(
                delayMillis = delayMillis + Random.nextLong(MAX_JITTER_MILLIS + 1),
                retry = retry,
            )
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
        val retries = pendingRetries.map { (owner, entries) -> owner to entries.toMap() }
        for ((owner, entries) in retries) {
            for ((taskKey, entry) in entries) retryEntry(owner, taskKey, entry)
        }
    }

    private suspend fun retryEntry(
        owner: Any,
        taskKey: String,
        entry: NetworkRetryEntry,
    ) {
        if (!runningRetries.add(entry)) return
        try {
            delay(entry.delayMillis)
            if (pendingRetries[owner]?.get(taskKey) !== entry) return
            entry.retry()
            if (pendingRetries[owner]?.get(taskKey) === entry) remove(owner, taskKey)
        } finally {
            runningRetries.remove(entry)
        }
    }

    private const val MAX_RETRY_ATTEMPTS = 3
    private const val INITIAL_DELAY_MILLIS = 1_000L
    private const val MAX_JITTER_MILLIS = 250L
}
