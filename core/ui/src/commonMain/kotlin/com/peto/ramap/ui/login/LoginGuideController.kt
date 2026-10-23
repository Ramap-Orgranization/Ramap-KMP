package com.peto.ramap.ui.login

import com.peto.ramap.domain.model.auth.LoginType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.jetbrains.compose.resources.StringResource

class LoginGuideController {
    private val currentRequest = MutableStateFlow<LoginGuideRequest?>(null)
    val request = currentRequest.asStateFlow()

    fun show(
        owner: Any,
        description: StringResource,
        onLoginTypeSelected: (LoginType) -> Unit,
        onDismiss: () -> Unit,
    ) {
        val next = LoginGuideRequest(owner, description, onLoginTypeSelected, onDismiss)
        val previous = currentRequest.getAndUpdate { next }
        // 같은 화면의 재요청은 기존 대기 동작을 취소하지 않는다.
        if (previous != null && previous.owner !== owner) previous.onDismiss()
    }

    fun select(
        request: LoginGuideRequest,
        type: LoginType,
    ) {
        if (currentRequest.compareAndSet(request, null)) request.onLoginTypeSelected(type)
    }

    fun dismiss(request: LoginGuideRequest) {
        if (currentRequest.compareAndSet(request, null)) request.onDismiss()
    }

    fun cancel(owner: Any) {
        val request = currentRequest.value ?: return
        if (request.owner === owner) dismiss(request)
    }
}
