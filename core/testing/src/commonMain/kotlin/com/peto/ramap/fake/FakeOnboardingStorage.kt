package com.peto.ramap.fake

import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import kotlinx.coroutines.CompletableDeferred

public class FakeOnboardingStorage : OnboardingStorage {
    public var confirmed: Boolean = false
    public var completed: Boolean = false
    public var selectionCount: Int? = null
    public var selectionFailure: Exception? = null
    public var pendingSelection: CompletableDeferred<Unit>? = null
    public var completionFailure: Exception? = null
    public var pendingCompletion: CompletableDeferred<Unit>? = null
    public var readFailure: Exception? = null
    public var saveFailure: Exception? = null
    public var pendingRead: CompletableDeferred<Unit>? = null
    public var pendingSave: CompletableDeferred<Unit>? = null

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

    override suspend fun isOnboardingCompleted(): Boolean = completed

    override suspend fun savedShopCount(): Int? = selectionCount

    override suspend fun saveShopSelection(savedShopCount: Int) {
        pendingSelection?.await()
        selectionFailure?.let { throw it }
        selectionCount = savedShopCount
    }

    override suspend fun completeOnboarding() {
        pendingCompletion?.await()
        completionFailure?.let { throw it }
        completed = true
        selectionCount = null
    }
}
