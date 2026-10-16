package com.peto.ramap.domain.model.personalization

/**
 * 매장별 개인화 기능에서 공유하는 현재 사용자 상태.
 *
 * 숨긴 매장은 북마크 및 알림 설정 집합에 포함되지 않는다.
 */
data class Personalization(
    val bookmarkedShopIds: Set<String> = emptySet(),
    val hiddenShopIds: Set<String> = emptySet(),
    val notificationShopIds: Set<String> = emptySet(),
) {
    fun visibilityChange(
        shopId: String?,
        previous: Personalization,
    ): VisibilityStatus =
        when (shopId) {
            null -> VisibilityStatus.Unchanged
            !in previous.hiddenShopIds if shopId in hiddenShopIds -> VisibilityStatus.Hidden
            in previous.hiddenShopIds if shopId !in hiddenShopIds -> VisibilityStatus.Visible
            else -> VisibilityStatus.Unchanged
        }

    fun bookmarkChangesSince(previousIds: Set<String>?): List<BookmarkChange> {
        if (previousIds == null) return emptyList()
        return (bookmarkedShopIds - previousIds).map { BookmarkChange(it, BookmarkChangeStatus.ADDED) } +
            (previousIds - bookmarkedShopIds).map { BookmarkChange(it, BookmarkChangeStatus.REMOVED) }
    }

    fun nextBookmarkEnabled(shopId: String): Boolean = shopId !in bookmarkedShopIds

    fun nextHiddenEnabled(shopId: String): Boolean = shopId !in hiddenShopIds

    fun hasBookmark(shopId: String): Boolean = shopId in bookmarkedShopIds

    /** 숨김 매장의 변경을 차단하고 그 외에는 현재 구독을 반전한다. */
    fun notificationToggle(shopId: String): NotificationStatus =
        if (shopId in hiddenShopIds) {
            NotificationStatus.Hidden
        } else {
            NotificationStatus.Allowed(enabled = shopId !in notificationShopIds)
        }

    /**
     * 특정 매장의 북마크 상태를 [isBookmarked]로 변경한다.
     *
     * 숨긴 매장은 북마크 설정 요청이 있어도 북마크에 포함하지 않는다.
     */
    fun changeBookmark(
        shopId: String,
        isBookmarked: Boolean,
    ): Personalization {
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
    fun addBookmarks(shopIds: Set<String>): Personalization = copy(bookmarkedShopIds = bookmarkedShopIds + (shopIds - hiddenShopIds))

    /**
     * 특정 매장의 알림 구독 상태를 [isSubscribed]로 변경한다.
     */
    fun changeNotificationSubscription(
        shopId: String,
        isSubscribed: Boolean,
    ): Personalization {
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
    fun hideShop(shopId: String): Personalization =
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

    /**
     * 북마크 뷰 또는 일반 뷰 조건 전환 시 특정 매장을 시트/화면에 유지해야 하는지 판단한다.
     */
    fun shouldKeepShop(
        shopId: String,
        willShowBookmarkedShops: Boolean,
    ): Boolean =
        if (willShowBookmarkedShops) {
            shopId in bookmarkedShopIds
        } else {
            shopId !in hiddenShopIds
        }
}
