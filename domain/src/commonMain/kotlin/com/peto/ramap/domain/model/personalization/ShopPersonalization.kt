package com.peto.ramap.domain.model.personalization

/**
 * 매장별 개인화 기능에서 공유하는 현재 사용자 상태.
 *
 * 숨긴 매장은 북마크 및 알림 설정 집합에 포함되지 않는다.
 */
data class ShopPersonalization(
    val bookmarkedShopIds: Set<String> = emptySet(),
    val hiddenShopIds: Set<String> = emptySet(),
    val notificationShopIds: Set<String> = emptySet(),
) {
    /**
     * 특정 매장의 북마크 상태를 [isBookmarked]로 변경한다.
     *
     * 숨긴 매장은 북마크 설정 요청이 있어도 북마크에 포함하지 않는다.
     */
    fun changeBookmark(
        shopId: String,
        isBookmarked: Boolean,
    ): ShopPersonalization {
        val changedBookmarkedShopIds =
            if (isBookmarked) {
                bookmarkedShopIds + shopId
            } else {
                bookmarkedShopIds - shopId
            }

        return copy(
            bookmarkedShopIds = changedBookmarkedShopIds - hiddenShopIds,
        )
    }

    /** 숨김 매장을 제외하고 여러 매장을 좋아요에 추가한다. */
    fun addBookmarks(shopIds: Set<String>): ShopPersonalization = copy(bookmarkedShopIds = bookmarkedShopIds + (shopIds - hiddenShopIds))

    /**
     * 특정 매장의 알림 구독 상태를 [isSubscribed]로 변경한다.
     */
    fun changeNotificationSubscription(
        shopId: String,
        isSubscribed: Boolean,
    ): ShopPersonalization {
        val changedNotificationShopIds =
            if (isSubscribed) {
                notificationShopIds + shopId
            } else {
                notificationShopIds - shopId
            }

        return copy(
            notificationShopIds = changedNotificationShopIds - hiddenShopIds,
        )
    }

    /**
     * 특정 매장을 숨기고 연관된 북마크와 알림 설정을 함께 제거한다.
     */
    fun hideShop(shopId: String): ShopPersonalization =
        copy(
            hiddenShopIds = hiddenShopIds + shopId,
            bookmarkedShopIds = bookmarkedShopIds - shopId,
            notificationShopIds = notificationShopIds - shopId,
        )

    /**
     * 숨긴 매장에 대한 알림 활성화 요청을 무시해야 하는지 판단한다.
     */
    fun shouldIgnoreNotificationUpdate(
        shopId: String,
        enabled: Boolean,
    ): Boolean = enabled && shopId in hiddenShopIds
}
