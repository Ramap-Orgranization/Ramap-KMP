package com.peto.ramap.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.peto.ramap.MainActivity
import com.peto.ramap.R
import com.peto.ramap.network.PushRegistrationCoordinator

class RamapFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        PushRegistrationCoordinator.track(installationId, PLATFORM_ANDROID, TARGET_TYPE_FID)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        val notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel(notificationManager)
        notificationManager.notify(
            message.messageId?.hashCode() ?: notification.hashCode(),
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setAutoCancel(true)
                .setContentIntent(createContentIntent())
                .build(),
        )
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.event_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    private fun createContentIntent(): PendingIntent {
        val launchIntent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        return PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val PLATFORM_ANDROID = "android"
        const val TARGET_TYPE_FID = "fid"
        const val CHANNEL_ID = "events"
    }
}
