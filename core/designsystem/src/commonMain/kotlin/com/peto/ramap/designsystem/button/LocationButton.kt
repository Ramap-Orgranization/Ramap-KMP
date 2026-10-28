package com.peto.ramap.designsystem.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.current_location
import ramap.shared.generated.resources.ic_current_location

@Composable
fun LocationButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCircleIconButton(
        icon = Res.drawable.ic_current_location,
        isActive = false,
        contentDescription = stringResource(Res.string.current_location),
        onClick = onClick,
        modifier = modifier,
        isLoading = isLoading,
    )
}
