package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.ShopEventNotificationSubscriptionResponse
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultSubscribedShopRepository(
    private val client: SupabaseClient,
) : SubscribedShopRepository {
    override suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>> =
        invokeRequest {
            client
                .from(SHOP_SUBSCRIPTION_TABLE)
                .select()
                .decodeList<ShopEventNotificationSubscriptionResponse>()
                .mapTo(mutableSetOf()) { it.shopId }
        }

    override suspend fun subscribeShop(shopId: String): RamapResult<Unit> = changeSubscription(shopId, enabled = true)

    override suspend fun unsubscribeShop(shopId: String): RamapResult<Unit> = changeSubscription(shopId, enabled = false)

    /**
     * 구독 변경 RPC에 요청 상태를 전달한다.
     */
    private suspend fun changeSubscription(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            client.postgrest.rpc(
                function = SET_SHOP_NOTIFICATION_RPC,
                parameters =
                    buildJsonObject {
                        put(SHOP_ID_PARAMETER, shopId)
                        put(ENABLED_PARAMETER, enabled)
                    },
            )
        }

    private companion object {
        private const val SET_SHOP_NOTIFICATION_RPC = "set_shop_event_notification"
        private const val SHOP_SUBSCRIPTION_TABLE = "shop_event_notification_subscriptions"
        private const val SHOP_ID_PARAMETER = "shop_id"
        private const val ENABLED_PARAMETER = "enabled"
    }
}
