package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.ui.registration.contract.AdminRegistrationTab
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_add
import ramap.shared.generated.resources.ic_list
import ramap.shared.generated.resources.ic_notification

@Composable
internal fun AdminBottomNavigation(
    selectedTab: AdminRegistrationTab,
    onTabSelected: (AdminRegistrationTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(CommonColor.White)
                .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(CommonColor.White),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminRegistrationTab.entries.forEach { tab ->
                AdminBottomNavigationItem(
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
private fun AdminBottomNavigationItem(
    tab: AdminRegistrationTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (selected) GrayColor.C500 else GrayColor.C300
    val icon =
        when (tab) {
            AdminRegistrationTab.EVENT_MANAGEMENT -> Res.drawable.ic_list
            AdminRegistrationTab.EVENT_REGISTRATION -> Res.drawable.ic_add
            AdminRegistrationTab.OPERATING_NOTICE -> Res.drawable.ic_notification
        }
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(color),
        )
        AppText(
            text = stringResource(labelResourceId(tab)),
            style = AppTextStyle.B4,
            color = color,
        )
    }
}

private fun labelResourceId(tab: AdminRegistrationTab): Int =
    when (tab) {
        AdminRegistrationTab.EVENT_MANAGEMENT -> R.string.admin_tab_event_management
        AdminRegistrationTab.EVENT_REGISTRATION -> R.string.admin_tab_event_registration
        AdminRegistrationTab.OPERATING_NOTICE -> R.string.admin_tab_operating_notice
    }

@Preview(showBackground = true)
@Composable
private fun AdminBottomNavigationPreview() {
    RamapTheme {
        AdminBottomNavigation(
            selectedTab = AdminRegistrationTab.EVENT_REGISTRATION,
            onTabSelected = {},
        )
    }
}
