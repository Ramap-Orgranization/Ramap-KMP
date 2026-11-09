package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationSettings

interface SubscribedShopRepository {
    suspend fun fetchNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings>

    /** 일반 저장은 숨김·비활성 선택을 보존하고, 명시적 전체 해제만 이를 제거한다. */
    suspend fun saveNotificationSettings(
        settings: NotificationSettings,
        selection: NotificationSelection,
        clearAll: Boolean = false,
    ): RamapResult<NotificationSettings>

    suspend fun clearNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings>

    suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>>

    /** 지정한 매장의 이벤트 알림을 구독한다. */
    suspend fun subscribeShop(shopId: String): RamapResult<Unit>

    /** 지정한 매장의 이벤트 알림 구독을 해제한다. */
    suspend fun unsubscribeShop(shopId: String): RamapResult<Unit>
}
