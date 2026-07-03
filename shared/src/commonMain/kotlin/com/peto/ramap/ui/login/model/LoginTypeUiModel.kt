package com.peto.ramap.ui.login.model

import androidx.compose.ui.graphics.Color
import com.peto.ramap.domain.model.LoginType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class LoginTypeUiModel(
    val type: LoginType,
    val logo: DrawableResource,
    val title: StringResource,
    val background: Color,
    val border: Color,
    val textColor: Color,
)
