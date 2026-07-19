package com.peto.ramap.domain.model.personalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopPersonalizationTest {
    @Test
    fun `숨긴 매장의 북마크를 활성화해도 북마크에 포함하지 않는다`() {
        val personalization = ShopPersonalization(hiddenShopIds = setOf(SHOP_ID))

        val result = personalization.changeBookmark(SHOP_ID, isBookmarked = true)

        assertFalse(SHOP_ID in result.bookmarkedShopIds)
    }

    @Test
    fun `매장을 숨기면 북마크와 알림 설정에서 함께 제거한다`() {
        val personalization =
            ShopPersonalization(
                bookmarkedShopIds = setOf(SHOP_ID, OTHER_SHOP_ID),
                notificationShopIds = setOf(SHOP_ID, OTHER_SHOP_ID),
            )

        val result = personalization.hideShop(SHOP_ID)

        assertTrue(SHOP_ID in result.hiddenShopIds)
        assertEquals(setOf(OTHER_SHOP_ID), result.bookmarkedShopIds)
        assertEquals(setOf(OTHER_SHOP_ID), result.notificationShopIds)
    }

    @Test
    fun `알림 설정 여부를 요청한 상태로 변경한다`() {
        val personalization = ShopPersonalization()

        val enabled =
            personalization.changeNotificationSubscription(SHOP_ID, isSubscribed = true)
        val disabled =
            enabled.changeNotificationSubscription(SHOP_ID, isSubscribed = false)

        assertTrue(SHOP_ID in enabled.notificationShopIds)
        assertFalse(SHOP_ID in disabled.notificationShopIds)
    }

    @Test
    fun `숨긴 매장의 알림을 활성화해도 알림 설정에 포함하지 않는다`() {
        val personalization = ShopPersonalization(hiddenShopIds = setOf(SHOP_ID))

        val result =
            personalization.changeNotificationSubscription(SHOP_ID, isSubscribed = true)

        assertFalse(SHOP_ID in result.notificationShopIds)
    }

    @Test
    fun `숨긴 매장의 알림 활성화 요청만 무시한다`() {
        val personalization = ShopPersonalization(hiddenShopIds = setOf(SHOP_ID))

        assertTrue(personalization.shouldIgnoreNotificationUpdate(SHOP_ID, enabled = true))
        assertFalse(personalization.shouldIgnoreNotificationUpdate(SHOP_ID, enabled = false))
        assertFalse(personalization.shouldIgnoreNotificationUpdate(OTHER_SHOP_ID, enabled = true))
    }

    private companion object {
        const val SHOP_ID = "shop-id"
        const val OTHER_SHOP_ID = "other-shop-id"
    }
}
