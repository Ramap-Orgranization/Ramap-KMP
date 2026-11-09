package com.peto.ramap.platform.permission

import com.peto.ramap.platform.NotificationPermissionRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class IosNotificationPermissionGenerator(
    private val scope: CoroutineScope,
    private val onResult: (Boolean) -> Unit,
) : NotificationPermissionGenerator {
    override fun requestPermission() {
        scope.launch {
            val granted =
                try {
                    NotificationPermissionRequester.request()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    false
                }
            onResult(granted)
        }
    }
}
