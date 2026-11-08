package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult

interface SubscribedShopRepository {
    suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>>

    /** 지정한 매장의 이벤트 알림을 구독한다. */
    suspend fun subscribeShop(shopId: String): RamapResult<Unit>

    /** 지정한 매장의 이벤트 알림 구독을 해제한다. */
    suspend fun unsubscribeShop(shopId: String): RamapResult<Unit>
}
