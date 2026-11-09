package com.peto.ramap.domain.store

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.NotificationSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * 여러 화면에서 공유하는 매장 개인화 상태의 단일 진실 공급원.
 *
 * [state]는 초기 동기화 상태와 성공한 개인화 값을 하나의 원자적인 상태로 제공한다.
 */
interface PersonalizationStore {
    fun publishNotificationSettings(settings: NotificationSettings): Boolean

    suspend fun clearShopNotifications(shopId: String): RamapResult<Unit>

    /** 현재 세션의 초기 동기화 상태와 성공한 개인화 값. */
    val state: StateFlow<PersonalizationBootstrapState>

    /**
     * 현재 사용자의 개인화 데이터를 다시 불러와 [PersonalizationBootstrapState.Success]로 한 번에 교체한다.
     *
     * 하나의 저장소라도 조회에 실패하면 기존 상태를 유지한다.
     */
    suspend fun refresh(): RamapResult<Unit>

    suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit>

    /** [shopId]의 북마크 여부를 [enabled]로 변경한다. */
    suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    /** 아직 좋아요하지 않은 매장을 좋아요에 추가한다. */
    suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit>

    /** [shopId]의 알림 설정 여부를 [enabled]로 변경한다. */
    suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    /** 사용자 세션이 종료될 때 공유 중인 개인화 상태를 비운다. */
    suspend fun clear()
}
