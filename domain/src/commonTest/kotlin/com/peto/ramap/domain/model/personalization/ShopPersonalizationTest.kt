package com.peto.ramap.domain.model.personalization

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopPersonalizationTest {
    @Test
    fun `알림 설정 여부를 요청한 상태로 변경한다`() {
        val personalization = Personalization()

        val enabled =
            personalization.changeNotificationSubscription(SHOP_ID, isSubscribed = true)
        val disabled =
            enabled.changeNotificationSubscription(SHOP_ID, isSubscribed = false)

        assertTrue(SHOP_ID in enabled.notificationShopIds)
        assertFalse(SHOP_ID in disabled.notificationShopIds)
    }

    private companion object {
        const val SHOP_ID = "shop-id"
    }
}
