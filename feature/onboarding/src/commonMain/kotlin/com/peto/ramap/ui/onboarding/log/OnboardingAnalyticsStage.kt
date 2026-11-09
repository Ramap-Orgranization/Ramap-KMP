package com.peto.ramap.ui.onboarding.log

internal enum class OnboardingAnalyticsStage(
    val key: String,
) {
    PermissionGuide("permission_guide"),
    NotificationMenu("notification_menu"),
    NotificationTypes("notification_types"),
    ReviewMap("review_map"),
    ShopSelection("shop_selection"),
}
