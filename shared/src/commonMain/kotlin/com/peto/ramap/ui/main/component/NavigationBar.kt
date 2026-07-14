package com.peto.ramap.ui.main.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.model.TabStatus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_event
import ramap.shared.generated.resources.ic_map
import ramap.shared.generated.resources.ic_map_selected
import ramap.shared.generated.resources.ic_person
import ramap.shared.generated.resources.top_level_tab_event
import ramap.shared.generated.resources.top_level_tab_map
import ramap.shared.generated.resources.top_level_tab_my

@Composable
fun NavigationBar(
    selectedTab: TabStatus,
    onTabSelected: (TabStatus) -> Unit,
) {
    Column {
        HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(CommonColor.White),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabStatus.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: TabStatus,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val icon =
        when (tab) {
            TabStatus.MAP -> if (selected) Res.drawable.ic_map_selected else Res.drawable.ic_map
            TabStatus.EVENT -> Res.drawable.ic_event
            TabStatus.MY -> Res.drawable.ic_person
        }
    val label =
        when (tab) {
            TabStatus.MAP -> stringResource(Res.string.top_level_tab_map)
            TabStatus.EVENT -> stringResource(Res.string.top_level_tab_event)
            TabStatus.MY -> stringResource(Res.string.top_level_tab_my)
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = interactionSource,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
            colorFilter = ColorFilter.tint(if (selected) GrayColor.C500 else GrayColor.C300),
        )

        AppText(
            text = label,
            color = if (selected) GrayColor.C500 else GrayColor.C300,
            style = AppTextStyle.B4,
        )
    }
}
