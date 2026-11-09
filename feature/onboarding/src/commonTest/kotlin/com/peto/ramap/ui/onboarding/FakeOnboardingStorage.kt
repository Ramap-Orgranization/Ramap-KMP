package com.peto.ramap.ui.onboarding

import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import kotlinx.coroutines.CompletableDeferred

internal class FakeOnboardingStorage : OnboardingStorage {
    var confirmed = false
    var readFailure: Exception? = null
    var saveFailure: Exception? = null
    var pendingRead: CompletableDeferred<Unit>? = null
    var pendingSave: CompletableDeferred<Unit>? = null

    override suspend fun isPermissionGuideConfirmed(): Boolean {
        pendingRead?.await()
        readFailure?.let { throw it }
        return confirmed
    }

    override suspend fun confirmPermissionGuide() {
        pendingSave?.await()
        saveFailure?.let { throw it }
        confirmed = true
    }
}
