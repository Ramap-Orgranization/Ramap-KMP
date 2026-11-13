package com.peto.ramap.ui.onboarding

import com.peto.ramap.analytics.AnalyticsTracker
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.store.PersonalizationStore
import com.peto.ramap.platform.storage.onboarding.OnboardingStorage
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.onboarding.contract.OnboardingIntent
import com.peto.ramap.ui.onboarding.contract.OnboardingLoadKey
import com.peto.ramap.ui.onboarding.contract.OnboardingSideEffect
import com.peto.ramap.ui.onboarding.contract.OnboardingStep
import com.peto.ramap.ui.onboarding.contract.OnboardingUiState
import com.peto.ramap.ui.onboarding.log.OnboardingAnalytics
import com.peto.ramap.ui.onboarding.permission.contract.OnboardingPermission
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.onboarding_completion_failure

class OnboardingViewModel(
    private val storage: OnboardingStorage,
    analyticsTracker: AnalyticsTracker,
    private val loginRepository: LoginRepository,
    private val personalizationStore: PersonalizationStore,
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
            is OnboardingIntent.ShopSelectionCompleted -> completeShopSelection(intent.savedShopCount)
            OnboardingIntent.LoginStepCompleted -> completeLoginStep()
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
            if (storage.isOnboardingCompleted()) {
                markCompleted()
                return@launchTask
            }

            val savedShopCount = storage.savedShopCount()
            val step =
                when {
                    savedShopCount != null -> OnboardingStep.Login(savedShopCount)
                    storage.isPermissionGuideConfirmed() -> OnboardingStep.Guide
                    else -> OnboardingStep.PermissionGuide
                }
            analytics.start(step)
            if (step is OnboardingStep.Login) restoreLoginStep(step) else reduce { copy(step = step) }
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
            storage.confirmPermissionGuide()

            reduce { copy(step = OnboardingStep.Guide) }
        }
    }

    private fun completeShopSelection(savedShopCount: Int) {
        if (currentState.step != OnboardingStep.Guide) return
        advanceToLogin(OnboardingStep.Login(savedShopCount))
    }

    private suspend fun restoreLoginStep(step: OnboardingStep.Login) {
        try {
            loginRepository.awaitInitialization()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // 기존 세션을 복원하지 못해도 로그인 선택 화면에서 시작할 수 있다.
        }
        advanceToLogin(step)
    }

    private fun advanceToLogin(step: OnboardingStep.Login) {
        if (loginRepository.hasSession()) {
            completeOnboarding(step)
        } else {
            reduce { copy(step = step) }
        }
    }

    private fun completeLoginStep() {
        val step = currentState.step as? OnboardingStep.Login ?: return
        completeOnboarding(step)
    }

    private fun completeOnboarding(step: OnboardingStep.Login) {
        launchTask(
            taskKey = COMPLETE_TASK_KEY,
            loadKey = OnboardingLoadKey.Complete,
            policy = TaskPolicy.IgnoreNew,
        ) {
            analytics.finishRequested(step.savedShopCount)
            try {
                storage.completeOnboarding()
                if (currentState.step is OnboardingStep.Login && loginRepository.hasSession()) {
                    personalizationStore.clear()
                }
                analytics.completed(step.savedShopCount)
                markCompleted()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                analytics.finishFailed(step.savedShopCount)
                handleError(exception)
                reduce { copy(step = step) }
                val toast = ToastData(Res.string.onboarding_completion_failure, ToastType.ERROR)
                postSideEffect(OnboardingSideEffect.ShowToast(toast))
            }
        }
    }

    private suspend fun markCompleted() {
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
        const val COMPLETE_TASK_KEY = "onboarding-complete"
    }
}
