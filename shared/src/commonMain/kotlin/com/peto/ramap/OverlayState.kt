package com.peto.ramap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.usecase.SignInUseCase
import com.peto.ramap.navigation.NavigationState
import com.peto.ramap.ui.login.LoginGuideController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_login_failure_message
import ramap.shared.generated.resources.kakao_login_failure_message
import ramap.shared.generated.resources.login_required_profile_description
import ramap.shared.generated.resources.login_success_message

internal class OverlayState(
    private val loginRepository: LoginRepository,
    private val navigationState: NavigationState,
    private val signInUseCase: SignInUseCase,
    private val toastManager: ToastManager,
    private val coroutineScope: CoroutineScope,
    private val loginGuideController: LoginGuideController,
) {
    var overlayProfileUserId by mutableStateOf<String?>(null)
    var overlayReviewWriteArgs by mutableStateOf<Pair<String, String?>?>(null)
    var pendingProfileTarget by mutableStateOf<Pair<String, Boolean>?>(null)

    val isOverlayActive: Boolean
        get() = overlayProfileUserId != null || overlayReviewWriteArgs != null

    fun openProfile(
        userId: String,
        asOverlay: Boolean,
    ) {
        if (userId.isBlank()) return
        if (!loginRepository.hasSession()) {
            pendingProfileTarget = userId to asOverlay
            loginGuideController.show(
                owner = this,
                description = Res.string.login_required_profile_description,
                onLoginTypeSelected = ::signIn,
                onDismiss = { pendingProfileTarget = null },
            )
            return
        }
        if (asOverlay) {
            overlayProfileUserId = userId
        } else {
            navigationState.showOtherReviews(userId)
        }
    }

    fun dismissLoginGuide() {
        loginGuideController.cancel(this)
    }

    fun signIn(type: LoginType) {
        coroutineScope.launch {
            val result = signInUseCase(type, AnalyticsSource.ACCOUNT)
            when (result) {
                is RamapResult.Success -> {
                    toastManager.show(
                        ToastData(
                            message = Res.string.login_success_message,
                            type = ToastType.SUCCESS,
                        ),
                    )
                }
                is RamapResult.Error -> {
                    pendingProfileTarget = null
                    val failureMessage =
                        when (type) {
                            LoginType.KAKAO -> Res.string.kakao_login_failure_message
                            LoginType.APPLE -> Res.string.apple_login_failure_message
                        }
                    toastManager.show(
                        ToastData(
                            message = failureMessage,
                            type = ToastType.ERROR,
                        ),
                    )
                }
            }
        }
    }
}
