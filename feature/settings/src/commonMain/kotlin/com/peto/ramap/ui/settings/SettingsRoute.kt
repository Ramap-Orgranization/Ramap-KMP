package com.peto.ramap.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.settings_account_menu
import ramap.shared.generated.resources.settings_information_menu
import ramap.shared.generated.resources.settings_notification_menu
import ramap.shared.generated.resources.settings_title

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onAccountNavigate: () -> Unit,
    onInformationNavigate: () -> Unit,
    onNotificationSettingsNavigate: () -> Unit,
) {
    SettingsPage(Res.string.settings_title, onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .border(1.dp, GrayColor.C200, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
        ) {
            visibleSettingsMenus(NotificationPermissionRequester.isSupported).forEachIndexed { index, menu ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = GrayColor.C200)
                SettingsRow(
                    title =
                        when (menu) {
                            SettingsMenu.ACCOUNT -> Res.string.settings_account_menu
                            SettingsMenu.INFORMATION -> Res.string.settings_information_menu
                            SettingsMenu.NOTIFICATION -> Res.string.settings_notification_menu
                        },
                    onClick =
                        when (menu) {
                            SettingsMenu.ACCOUNT -> onAccountNavigate
                            SettingsMenu.INFORMATION -> onInformationNavigate
                            SettingsMenu.NOTIFICATION -> onNotificationSettingsNavigate
                        },
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: StringResource,
    onClick: () -> Unit,
) {
    AppText(
        text = stringResource(title),
        style = AppTextStyle.B1,
        color = GrayColor.C500,
        modifier = Modifier.fillMaxWidth().noRippleClickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 18.dp),
    )
}
