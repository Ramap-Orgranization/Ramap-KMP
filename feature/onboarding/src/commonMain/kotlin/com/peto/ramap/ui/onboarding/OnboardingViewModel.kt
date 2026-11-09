package com.peto.ramap.ui.onboarding

import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

internal class OnboardingViewModel(
    private val storage: OnboardingStorage,
) : BaseViewModel<OnboardingUiState, OnboardingIntent, OnboardingSideEffect>(OnboardingUiState()) {
    private var pendingPermission: OnboardingPermission? = null
    private var pendingPermissionResult: CompletableDeferred<Unit>? = null

    init {
        readConfirmation()
    }

    override suspend fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.ConfirmPermissionGuide -> confirmPermissionGuide()
            OnboardingIntent.ShopSelectionCompleted -> reduce { copy(hasCompletedOnboarding = true) }
            is OnboardingIntent.PermissionRequestFinished -> finishPermissionRequest(intent.permission)
        }
    }

    private fun readConfirmation() {
        launchTask(
            taskKey = READ_CONFIRMATION_TASK_KEY,
            loadKey = OnboardingLoadKey.ReadConfirmation,
        ) {
            val hasConfirmed =
                try {
                    storage.isPermissionGuideConfirmed()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    handleError(exception)
                    false
                }
            val hasCompleted =
                try {
                    storage.isOnboardingCompleted()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (exception: Exception) {
                    handleError(exception)
                    false
                }
            reduce { copy(hasConfirmedPermissionGuide = hasConfirmed, hasCompletedOnboarding = hasCompleted) }
        }
    }

    private fun confirmPermissionGuide() {
        if (currentState.hasCompletedOnboarding || currentState.hasConfirmedPermissionGuide || currentState.loadState.isLoading(OnboardingLoadKey.ReadConfirmation)) return

        launchTask(
            taskKey = SAVE_CONFIRMATION_TASK_KEY,
            loadKey = OnboardingLoadKey.ConfirmPermissionGuide,
            policy = TaskPolicy.IgnoreNew,
        ) {
            requestPermission(OnboardingPermission.Location)
            requestPermission(OnboardingPermission.Notification)
            try {
                storage.confirmPermissionGuide()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                handleError(exception)
                // 안내 기록을 저장하지 못해도 현재 실행에서 앱 이용을 막지 않는다.
            }
            reduce { copy(hasConfirmedPermissionGuide = true) }
        }
    }

    private suspend fun requestPermission(permission: OnboardingPermission) {
        val result = CompletableDeferred<Unit>()
        pendingPermission = permission
        pendingPermissionResult = result
        try {
            postSideEffect(OnboardingSideEffect.RequestPermission(permission))
            result.await()
        } finally {
            pendingPermission = null
            pendingPermissionResult = null
        }
    }

    private fun finishPermissionRequest(permission: OnboardingPermission) {
        if (permission != pendingPermission) return
        pendingPermissionResult?.complete(Unit)
    }

    private companion object {
        const val READ_CONFIRMATION_TASK_KEY = "onboarding-read-permission-guide"
        const val SAVE_CONFIRMATION_TASK_KEY = "onboarding-confirm-permission-guide"
    }
}
