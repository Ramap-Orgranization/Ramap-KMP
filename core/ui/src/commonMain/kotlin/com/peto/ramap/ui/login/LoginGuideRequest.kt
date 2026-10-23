package com.peto.ramap.ui.login

import com.peto.ramap.domain.model.auth.LoginType
import org.jetbrains.compose.resources.StringResource

class LoginGuideRequest internal constructor(
    internal val owner: Any,
    val description: StringResource,
    internal val onLoginTypeSelected: (LoginType) -> Unit,
    internal val onDismiss: () -> Unit,
)
