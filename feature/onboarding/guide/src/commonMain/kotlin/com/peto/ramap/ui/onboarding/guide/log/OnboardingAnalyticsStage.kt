package com.peto.ramap.ui.onboarding.guide.log

public enum class OnboardingAnalyticsStage(
    val key: String,
    val isGuidePage: Boolean = false,
) {
    PermissionGuide("permission_guide"),
    NotificationMenu("notification_menu", isGuidePage = true),
    NotificationTypes("notification_types", isGuidePage = true),
    ReviewMap("review_map", isGuidePage = true),
    ShopSelection("shop_selection"),
    Login("login"),
}
