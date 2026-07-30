package com.peto.ramap.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.peto.ramap.MainActivity
import com.peto.ramap.R
import com.peto.ramap.notification.DeepLinkKey.NOTIFICATION_DEEP_LINK_KEY
import org.koin.android.ext.android.inject

/**
 * Firebase Cloud Messaging으로 받은 이벤트 알림을 Android 시스템 알림으로 표시한다.
 *
 * 이 서비스는 Manifest에서 `com.google.firebase.MESSAGING_EVENT`를 처리하며 외부 앱이 직접
 * 실행하지 못하도록 `exported=false`로 선언되어 있다. [onMessageReceived] 호출 자체에는 알림
 * 런타임 권한이 필요하지 않지만, Android 13(API 33) 이상에서는 사용자가
 * `android.permission.POST_NOTIFICATIONS` 권한을 허용해야 [NotificationManager.notify]로 게시한
 * 알림이 표시된다. 권한 요청은 앱 화면의 `NotificationPermissionRequester`가 담당한다.
 *
 * 알림을 누르면 [MainActivity]로 딥 링크 값을 전달한다. 이때 Activity Intent와 PendingIntent의
 * 플래그가 기존 Activity 재사용, 최신 딥 링크 반영, 외부 변경 방지를 각각 담당한다.
 */
class RamapFirebaseMessagingService : FirebaseMessagingService() {
    private val notificationRegistry: NotificationRegistry by inject()

    /**
     * Firebase Installation ID가 등록되면 푸시 발송 대상으로 사용할 수 있도록 서버에 기록한다.
     *
     * 알림 표시 권한과 무관한 등록 절차이므로 `POST_NOTIFICATIONS` 권한이 없어도 호출될 수 있다.
     *
     * @param installationId Firebase가 현재 앱 설치에 부여한 고유 식별자
     */
    override fun onRegistered(installationId: String) {
        notificationRegistry.track(installationId, PLATFORM_ANDROID, TARGET_TYPE_FID)
    }

    /**
     * 수신한 FCM 메시지의 notification payload를 Android 시스템 알림으로 게시한다.
     *
     * data-only 메시지처럼 notification payload가 없으면 처리하지 않는다. Android 13(API 33)
     * 이상에서는 이 메서드가 호출되더라도 `POST_NOTIFICATIONS` 권한이 없으면 알림이 사용자에게
     * 표시되지 않는다. 알림 채널은 게시 전에 필요할 때 생성하며, data payload의 `deep_link`는
     * 알림 클릭 시 [MainActivity]에 전달한다.
     *
     * @param message Firebase에서 수신한 메시지와 notification/data payload
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        logNotificationReceived(
            message.messageId.orEmpty(),
            if (message.data.containsKey(NOTIFICATION_DEEP_LINK_KEY)) 1L else 0L,
        )
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
                .setContentIntent(createContentIntent(message.data[NOTIFICATION_DEEP_LINK_KEY]))
                .build(),
        )
    }

    private fun logNotificationReceived(
        messageId: String,
        hasDeepLinkKey: Long,
    ) {
        FirebaseAnalytics.getInstance(this).logEvent("notification_received") {
            param(ANALYTICS_KEY_MESSAGE_ID, messageId)
            param(ANALYTICS_KEY_HAS_DEEP_LINK, hasDeepLinkKey)
        }
    }

    /**
     * Android 8.0(API 26) 이상에서 이벤트 알림용 채널을 생성한다.
     *
     * 같은 ID의 채널이 이미 존재하면 시스템이 기존 채널을 유지하므로 반복 호출해도 안전하다.
     * 채널 생성 자체에는 `POST_NOTIFICATIONS` 권한이 필요하지 않다.
     *
     * @param notificationManager 알림 채널을 등록할 시스템 서비스
     */
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

    /**
     * 알림을 눌렀을 때 [MainActivity]를 여는 변경 불가능한 [PendingIntent]를 만든다.
     *
     * [Intent.FLAG_ACTIVITY_SINGLE_TOP]은 대상 Activity가 현재 태스크의 최상단에 있으면 새
     * 인스턴스 대신 기존 인스턴스의 `onNewIntent`로 Intent를 전달한다. Manifest의 `singleTask`
     * 실행 모드도 기존 [MainActivity] 인스턴스를 재사용하므로, 백 스택에 같은 Activity가 중복으로
     * 쌓이지 않는다.
     *
     * [PendingIntent.FLAG_UPDATE_CURRENT]는 같은 요청 코드의 PendingIntent가 이미 있을 때 딥 링크
     * extra를 최신 값으로 갱신한다. [PendingIntent.FLAG_IMMUTABLE]은 생성 후 다른 주체가 내부
     * Intent를 변경하지 못하게 하며 Android 12(API 31) 이상의 mutability 지정 요구도 충족한다.
     */
    private fun createContentIntent(deepLink: String?): PendingIntent {
        val launchIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(NOTIFICATION_DEEP_LINK_KEY, deepLink)
            }
        return PendingIntent.getActivity(
            this,
            deepLink.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        private const val PLATFORM_ANDROID = "android"
        private const val TARGET_TYPE_FID = "fid"
        private const val CHANNEL_ID = "events"

        private const val ANALYTICS_KEY_MESSAGE_ID = "message_id"
        private const val ANALYTICS_KEY_HAS_DEEP_LINK = "has_deep_link"
    }
}
