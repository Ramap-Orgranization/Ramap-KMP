package com.peto.ramap.ui.onboarding

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingPermission
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.log.OnboardingAnalytics
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

class OnboardingViewModel(
    private val storage: OnboardingStorage,
    analyticsTracker: AnalyticsTracker,
) : BaseViewModel<OnboardingUiState, OnboardingIntent, OnboardingSideEffect>(OnboardingUiState()) {
    internal val analytics = OnboardingAnalytics(analyticsTracker)
    private var pendingPermission: OnboardingPermission? = null
    private var pendingPermissionResult: CompletableDeferred<Unit>? = null

    init {
        readConfirmation()
    }

    override suspend fun handleIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.ConfirmPermissionGuide -> confirmPermissionGuide()
            OnboardingIntent.ShopSelectionCompleted -> completeShopSelection()
            is OnboardingIntent.LocationPermissionFinished ->
                finishPermissionRequest(OnboardingPermission.Location) { analytics.locationResult(intent.status) }
            is OnboardingIntent.NotificationPermissionFinished ->
                finishPermissionRequest(OnboardingPermission.Notification) { analytics.notificationResult(intent.outcome) }
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
            val step =
                when {
                    hasCompleted -> OnboardingStep.Completed
                    hasConfirmed -> OnboardingStep.Guide
                    else -> OnboardingStep.PermissionGuide
                }
            if (!hasCompleted) analytics.start(resumesAfterPermissions = hasConfirmed)
            reduce { copy(step = step) }
            if (step == OnboardingStep.Completed) postSideEffect(OnboardingSideEffect.Completed)
        }
    }

    private fun confirmPermissionGuide() {
        if (currentState.step != OnboardingStep.PermissionGuide) return

        launchTask(
            taskKey = SAVE_CONFIRMATION_TASK_KEY,
            loadKey = OnboardingLoadKey.ConfirmPermissionGuide,
            policy = TaskPolicy.IgnoreNew,
        ) {
            analytics.permissionGuideConfirmed()
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
            reduce { copy(step = OnboardingStep.Guide) }
        }
    }

    private suspend fun completeShopSelection() {
        if (currentState.step != OnboardingStep.Guide) return
        reduce { copy(step = OnboardingStep.Completed) }
        postSideEffect(OnboardingSideEffect.Completed)
    }

    private suspend fun requestPermission(permission: OnboardingPermission) {
        val result = CompletableDeferred<Unit>()
        pendingPermission = permission
        pendingPermissionResult = result
        try {
            analytics.permissionRequested(permission)
            postSideEffect(OnboardingSideEffect.RequestPermission(permission))
            result.await()
        } finally {
            pendingPermission = null
            pendingPermissionResult = null
        }
    }

    private fun finishPermissionRequest(
        permission: OnboardingPermission,
        onResult: () -> Unit,
    ) {
        if (permission != pendingPermission) return
        val result = pendingPermissionResult ?: return
        if (result.isCompleted) return
        onResult()
        result.complete(Unit)
    }

    private companion object {
        const val READ_CONFIRMATION_TASK_KEY = "onboarding-read-permission-guide"
        const val SAVE_CONFIRMATION_TASK_KEY = "onboarding-confirm-permission-guide"
    }
}
