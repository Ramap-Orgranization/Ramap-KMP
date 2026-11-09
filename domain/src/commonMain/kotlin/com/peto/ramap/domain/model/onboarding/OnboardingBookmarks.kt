package com.peto.ramap.domain.model.onboarding

class OnboardingBookmarks(
    shopIds: Set<String>,
) {
    private val shopIds = shopIds.toSet()

    val savedCount: Int get() = shopIds.size
    val canComplete: Boolean get() = savedCount >= REQUIRED_SHOP_COUNT
    val remainingCount: Int get() = (REQUIRED_SHOP_COUNT - savedCount).coerceAtLeast(0)

    operator fun contains(shopId: String): Boolean = shopId in shopIds

    companion object {
        private const val REQUIRED_SHOP_COUNT = 3
    }
}
