package com.peto.ramap.ui.refresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Navigation entries can remain resumed underneath an app-owned overlay. */
@Composable
fun RefreshOnReturnEffect(
    isUncovered: Boolean = true,
    onReturn: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val refresh by rememberUpdatedState(onReturn)
    var hasBeenVisible by rememberSaveable { mutableStateOf(false) }
    var wasVisible by remember { mutableStateOf(false) }
    val isVisible = isUncovered && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(isVisible) {
        if (isVisible && !wasVisible) {
            if (hasBeenVisible) refresh()
            hasBeenVisible = true
        }
        wasVisible = isVisible
    }
}
