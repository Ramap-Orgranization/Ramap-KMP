package com.peto.ramap.domain.store

import com.peto.ramap.core.result.RamapResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 여러 화면에서 공유하는 매장 개인화 상태의 단일 진실 공급원.
 *
 * [state]는 초기 동기화 상태와 성공한 개인화 값을 하나의 원자적인 상태로 제공한다.
 */
interface ShopPersonalizationStore {
    /** 현재 세션의 초기 동기화 상태와 성공한 개인화 값. */
    val state: StateFlow<PersonalizationBootstrapState>

    /**
     * 원격 저장소의 개인화 데이터를 다시 불러와 [PersonalizationBootstrapState.Success]로 한 번에 교체한다.
     *
     * 하나의 저장소라도 조회에 실패하면 기존 상태를 유지한다.
     */
    suspend fun refresh(): RamapResult<Unit>

    /** [shopId]의 북마크 여부를 [enabled]로 변경한다. */
    suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    /**
     * [shopId]를 숨기고 해당 매장의 북마크와 알림 설정도 함께 해제한다.
     *
     * 원격 반영 중 실패하면 완료된 변경을 가능한 범위에서 보상하고 이전 상태로 복구한다.
     */
    suspend fun hideShop(shopId: String): RamapResult<Unit>

    /**
     * [shopId]의 숨김 상태만 해제한다.
     *
     * 숨김으로 함께 해제됐던 북마크와 알림 설정은 복구하지 않는다.
     */
    suspend fun unhideShop(shopId: String): RamapResult<Unit>

    /**
     * [shopId]의 알림 설정 여부를 [enabled]로 변경한다.
     *
     * 숨긴 매장의 알림 활성화 요청은 상태 및 원격 저장소를 변경하지 않는다.
     */
    suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    /** 사용자 세션이 종료될 때 공유 중인 개인화 상태를 비운다. */
    suspend fun clear()
}
