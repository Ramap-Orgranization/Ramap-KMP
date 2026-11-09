package com.peto.ramap.platform

import com.peto.ramap.platform.permission.NotificationPermissionOutcome
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual object NotificationPermissionRequester {
    actual val isSupported = true

    actual suspend fun isGranted(): Boolean = authorizationStatus().isAllowed()

    actual suspend fun request(): Boolean = requestOutcome().isGranted

    internal suspend fun requestOutcome(): NotificationPermissionOutcome {
        val status = authorizationStatus()
        val outcome =
            when {
                status.isAllowed() -> NotificationPermissionOutcome.AlreadyGranted
                status == UNAuthorizationStatusDenied -> NotificationPermissionOutcome.DisabledInSettings
                status == UNAuthorizationStatusNotDetermined -> requestAuthorization()
                else -> NotificationPermissionOutcome.Failed
            }
        if (outcome.isGranted) notifyToRegisterForRemoteNotifications()
        return outcome
    }

    private suspend fun authorizationStatus(): UNAuthorizationStatus? =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings -> continuation.resume(settings?.authorizationStatus) }
        }

    private fun UNAuthorizationStatus?.isAllowed(): Boolean = this == UNAuthorizationStatusAuthorized || this == UNAuthorizationStatusProvisional || this == UNAuthorizationStatusEphemeral

    private suspend fun requestAuthorization(): NotificationPermissionOutcome {
        val options =
            UNAuthorizationOptionAlert or
                UNAuthorizationOptionBadge or
                UNAuthorizationOptionSound
        return suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { granted, error ->
                    val outcome =
                        when {
                            error != null -> NotificationPermissionOutcome.Failed
                            granted -> NotificationPermissionOutcome.Granted
                            else -> NotificationPermissionOutcome.NotGranted
                        }
                    continuation.resume(outcome)
                }
        }
    }

    private fun notifyToRegisterForRemoteNotifications() {
        NSNotificationCenter.defaultCenter.postNotificationName(
            REGISTER_FOR_REMOTE_NOTIFICATIONS,
            null,
        )
    }

    private const val REGISTER_FOR_REMOTE_NOTIFICATIONS = "RegisterForRemoteNotifications"
}
