package com.peto.ramap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.app_exit_back_message
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val EXIT_INTERVAL = 2.seconds

@Composable
fun BackPressController(
    toastManager: ToastManager,
    navigationState: NavigationState,
    onExitRequested: (() -> Unit)?,
) {
    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    var previousBackPress by remember { mutableStateOf(TimeSource.Monotonic.markNow() - EXIT_INTERVAL) }

    val isBackHandlingEnabled = navigationState.canNavigateBack || onExitRequested != null

    NavigationBackHandler(
        state = backEventState,
        isBackEnabled = isBackHandlingEnabled,
        onBackCompleted = {
            val canPopBack = navigationState.canNavigateBack
            if (canPopBack) {
                navigationState.pop()
                return@NavigationBackHandler
            }

            val now = TimeSource.Monotonic.markNow()
            val isWithinExitInterval = now - previousBackPress <= EXIT_INTERVAL
            if (isWithinExitInterval) {
                onExitRequested?.invoke()
            } else {
                previousBackPress = now
                toastManager.tryShow(
                    ToastData(
                        message = Res.string.app_exit_back_message,
                        type = ToastType.DEFAULT,
                    ),
                )
            }
        },
    )
}
