package com.peto.ramap.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.privacy_policy_menu
import ramap.shared.generated.resources.settings_account_menu
import ramap.shared.generated.resources.settings_contact_menu
import ramap.shared.generated.resources.settings_notification_menu
import ramap.shared.generated.resources.settings_official_account_menu
import ramap.shared.generated.resources.settings_title

private const val OFFICIAL_ACCOUNT_URL = "https://www.instagram.com/ramap.app/"
private const val CONTACT_EMAIL_URL = "mailto:uni070@naver.com"
private const val PRIVACY_POLICY_URL = "https://ramap-orgranization.github.io/Ramap-KMP/"

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onAccountNavigate: () -> Unit,
    onNotificationSettingsNavigate: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val menus =
        SettingsMenu.visibleSettingsMenus(
            isLoggedIn = state.isLoggedIn,
            isNotificationSupported = NotificationPermissionRequester.isSupported,
        )
    SettingsPage(Res.string.settings_title, onBack) {
        Column(Modifier.fillMaxWidth()) {
            menus.forEachIndexed { index, menu ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
                SettingsRow(
                    title =
                        when (menu) {
                            SettingsMenu.ACCOUNT -> Res.string.settings_account_menu
                            SettingsMenu.NOTIFICATION -> Res.string.settings_notification_menu
                            SettingsMenu.OFFICIAL_ACCOUNT -> Res.string.settings_official_account_menu
                            SettingsMenu.CONTACT -> Res.string.settings_contact_menu
                            SettingsMenu.PRIVACY_POLICY -> Res.string.privacy_policy_menu
                        },
                    onClick =
                        {
                            when (menu) {
                                SettingsMenu.ACCOUNT -> onAccountNavigate()
                                SettingsMenu.NOTIFICATION -> onNotificationSettingsNavigate()
                                SettingsMenu.OFFICIAL_ACCOUNT -> ExternalUriOpener.open(OFFICIAL_ACCOUNT_URL)
                                SettingsMenu.CONTACT -> ExternalUriOpener.open(CONTACT_EMAIL_URL)
                                SettingsMenu.PRIVACY_POLICY -> ExternalUriOpener.open(PRIVACY_POLICY_URL)
                            }
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .noRippleClickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = stringResource(title),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    }
}
