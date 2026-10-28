package com.peto.ramap.designsystem.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.peto.ramap.theme.CommonColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_map_selected
import ramap.shared.generated.resources.ic_visited_pin
import ramap.shared.generated.resources.top_level_tab_map
import ramap.shared.generated.resources.visited_shops

@Composable
fun MapModeSwitchFab(
    isVisitedMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCircleIconButton(
        icon = if (isVisitedMode) Res.drawable.ic_map_selected else Res.drawable.ic_visited_pin,
        isActive = isVisitedMode,
        selected = isVisitedMode,
        contentDescription = stringResource(if (isVisitedMode) Res.string.top_level_tab_map else Res.string.visited_shops),
        onClick = onClick,
        modifier = modifier,
        tint = if (isVisitedMode) CommonColor.White else null,
    )
}
