package com.peto.ramap.domain.model.personalization

/**
 * 매장별 개인화 기능에서 공유하는 현재 사용자 상태.
 */
data class Personalization(
    val bookmarkedShopIds: Set<String> = emptySet(),
    val notificationShopIds: Set<String> = emptySet(),
) {
    fun bookmarkChangesSince(previousIds: Set<String>?): List<BookmarkChange> {
        if (previousIds == null) return emptyList()
        return (bookmarkedShopIds - previousIds).map { BookmarkChange(it, BookmarkChangeStatus.ADDED) } +
            (previousIds - bookmarkedShopIds).map { BookmarkChange(it, BookmarkChangeStatus.REMOVED) }
    }

    fun nextBookmarkEnabled(shopId: String): Boolean = shopId !in bookmarkedShopIds

    fun hasBookmark(shopId: String): Boolean = shopId in bookmarkedShopIds

    fun nextNotificationEnabled(shopId: String): Boolean = shopId !in notificationShopIds

    /** 특정 매장의 북마크 상태를 [isBookmarked]로 변경한다. */
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
            bookmarkedShopIds = changedBookmarkedShopIds,
        )
    }

    /** 여러 매장을 좋아요에 추가한다. */
    fun addBookmarks(shopIds: Set<String>): Personalization = copy(bookmarkedShopIds = bookmarkedShopIds + shopIds)

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
            notificationShopIds = changedNotificationShopIds,
        )
    }
}
