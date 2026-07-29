package com.peto.ramap.ui.resource

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

@Composable
fun UiText.format(): String =
    stringResource(
        resource = resource,
        formatArgs = arguments.toTypedArray(),
    )
