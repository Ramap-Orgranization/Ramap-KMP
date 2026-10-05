package com.peto.ramap.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual object NotificationPermissionRequester {
    actual val isSupported = true

    actual suspend fun isGranted(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val status = settings?.authorizationStatus
                    continuation.resume(
                        status == UNAuthorizationStatusAuthorized ||
                            status == UNAuthorizationStatusProvisional ||
                            status == UNAuthorizationStatusEphemeral,
                    )
                }
        }

    actual suspend fun request(): Boolean {
        if (isGranted()) {
            notifyToRegisterForRemoteNotifications()
            return true
        }

        val options =
            UNAuthorizationOptionAlert or
                UNAuthorizationOptionBadge or
                UNAuthorizationOptionSound
        val granted =
            suspendCancellableCoroutine { continuation ->
                UNUserNotificationCenter
                    .currentNotificationCenter()
                    .requestAuthorizationWithOptions(options) { isGranted, _ ->
                        continuation.resume(isGranted)
                    }
            }
        if (granted) notifyToRegisterForRemoteNotifications()
        return granted
    }

    private fun notifyToRegisterForRemoteNotifications() {
        NSNotificationCenter.defaultCenter.postNotificationName(
            REGISTER_FOR_REMOTE_NOTIFICATIONS,
            null,
        )
    }

    private const val REGISTER_FOR_REMOTE_NOTIFICATIONS = "RegisterForRemoteNotifications"
}
