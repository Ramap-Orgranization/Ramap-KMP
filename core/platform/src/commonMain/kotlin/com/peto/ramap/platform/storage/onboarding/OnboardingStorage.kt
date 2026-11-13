package com.peto.ramap.platform.storage.onboarding

interface OnboardingStorage {
    suspend fun isPermissionGuideConfirmed(): Boolean

    suspend fun confirmPermissionGuide()

    suspend fun savedShopCount(): Int?

    suspend fun saveShopSelection(savedShopCount: Int)

    suspend fun isOnboardingCompleted(): Boolean

    suspend fun completeOnboarding()
}
