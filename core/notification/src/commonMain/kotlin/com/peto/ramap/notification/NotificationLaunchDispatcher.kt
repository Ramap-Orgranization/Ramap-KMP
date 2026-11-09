package com.peto.ramap.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationLaunchDispatcher(
    private val deepLinkParser: NotificationDeepLinkParser,
) {
    private val mutablePendingDeepLink = MutableStateFlow<NotificationLaunch?>(null)
    private val receivedIds = linkedSetOf<String>()
    val pendingDeepLink = mutablePendingDeepLink.asStateFlow()

    fun dispatch(
        deepLink: String?,
        notificationId: String? = null,
    ) {
        val destination = deepLinkParser.parse(deepLink) ?: return
        val identity = notificationId ?: "legacy:$deepLink"
        if (notificationId != null && (notificationId.length !in 1..MAX_ID_LENGTH || notificationId.any { it.code !in 33..126 })) return
        if (!receivedIds.add(identity)) return
        if (receivedIds.size > MAX_RECENT_IDENTITIES) receivedIds.remove(receivedIds.first())
        mutablePendingDeepLink.value = if (destination == NotificationDeepLink.None) null else NotificationLaunch(identity, destination)
    }

    fun consume(launch: NotificationLaunch) {
        mutablePendingDeepLink.compareAndSet(launch, null)
    }

    private companion object {
        const val MAX_RECENT_IDENTITIES = 256
        const val MAX_ID_LENGTH = 256
    }
}
