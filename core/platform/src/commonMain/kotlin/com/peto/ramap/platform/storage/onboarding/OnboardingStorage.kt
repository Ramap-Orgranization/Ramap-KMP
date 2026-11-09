package com.peto.ramap.platform.storage.onboarding

interface OnboardingStorage {
    suspend fun isPermissionGuideConfirmed(): Boolean

    suspend fun confirmPermissionGuide()
}
