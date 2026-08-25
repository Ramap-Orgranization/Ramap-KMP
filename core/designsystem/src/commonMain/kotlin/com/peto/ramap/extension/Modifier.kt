package com.peto.ramap.extension

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier =
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = null,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
