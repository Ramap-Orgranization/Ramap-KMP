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
    actual suspend fun isGranted(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                val isGranted =
                    settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                        settings?.authorizationStatus == UNAuthorizationStatusProvisional ||
                        settings?.authorizationStatus == UNAuthorizationStatusEphemeral
                if (continuation.isActive) continuation.resume(isGranted)
            }
        }

    actual suspend fun request(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
            ) { granted, _ ->
                if (granted) {
                    NSNotificationCenter.defaultCenter.postNotificationName("NotificationPermissionGranted", null)
                }
                if (continuation.isActive) continuation.resume(granted)
            }
        }
}
