package com.peto.ramap.platform.permission

import com.peto.ramap.platform.NotificationPermissionRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class IosNotificationPermissionGenerator(
    private val scope: CoroutineScope,
    private val onResult: (NotificationPermissionOutcome) -> Unit,
) : NotificationPermissionGenerator {
    override fun requestPermission() {
        scope.launch {
            val outcome =
                try {
                    NotificationPermissionRequester.requestOutcome()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    NotificationPermissionOutcome.Failed
                }
            onResult(outcome)
        }
    }
}
