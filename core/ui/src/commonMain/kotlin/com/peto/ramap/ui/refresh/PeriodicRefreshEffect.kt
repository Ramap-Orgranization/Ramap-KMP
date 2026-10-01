package com.peto.ramap.ui.refresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PeriodicRefreshEffect(
    intervalMillis: Long,
    onRefresh: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val refresh by rememberUpdatedState(onRefresh)
    LaunchedEffect(lifecycleOwner, intervalMillis) {
        runPeriodicRefresh(lifecycleOwner.lifecycle, intervalMillis) { refresh() }
    }
}

internal suspend fun runPeriodicRefresh(
    lifecycle: Lifecycle,
    intervalMillis: Long,
    onRefresh: () -> Unit,
) {
    require(intervalMillis > 0)
    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (currentCoroutineContext().isActive) {
            delay(intervalMillis)
            onRefresh()
        }
    }
}
