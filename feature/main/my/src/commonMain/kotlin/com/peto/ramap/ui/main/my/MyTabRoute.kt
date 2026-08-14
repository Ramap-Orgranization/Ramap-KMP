package com.peto.ramap.ui.main.my

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.settings_account_menu
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.settings_information_menu
import ramap.shared.generated.resources.settings_notification_menu
import ramap.shared.generated.resources.settings_report_menu
import ramap.shared.generated.resources.settings_subscribed_shops_menu
import ramap.shared.generated.resources.settings_title

@Composable
fun MyTabRoute(
    onAccountNavigate: () -> Unit,
    onInformationNavigate: () -> Unit,
    onNotificationSettingsNavigate: () -> Unit,
    onReportNavigate: () -> Unit,
    onHiddenShopsNavigate: () -> Unit,
    onSubscribedShopsNavigate: () -> Unit,
    onBookmarkedShopsNavigate: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyContent(
        isLoggedIn = uiState.isLoggedIn,
        isNotificationSupported = NotificationPermissionRequester.isSupported,
        onAccountClick = onAccountNavigate,
        onInformationClick = onInformationNavigate,
        onNotificationSettingsClick = onNotificationSettingsNavigate,
        onReportClick = onReportNavigate,
        onHiddenShopsClick = onHiddenShopsNavigate,
        onSubscribedShopsClick = onSubscribedShopsNavigate,
        onBookmarkedShopsClick = onBookmarkedShopsNavigate,
    )
}

@Composable
internal fun MyContent(
    isLoggedIn: Boolean,
    isNotificationSupported: Boolean = true,
    onAccountClick: () -> Unit,
    onInformationClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onReportClick: () -> Unit,
    onHiddenShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onBookmarkedShopsClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                .padding(horizontal = 20.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_title),
            left = {},
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = GrayColor.C200, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
        ) {
            visibleSettingsMenus(isLoggedIn, isNotificationSupported).forEachIndexed { index, menu ->
                if (index > 0) {
                    HorizontalDivider(thickness = 1.dp, color = GrayColor.C200)
                }
                SettingsRow(
                    title = settingsMenuTitle(menu),
                    onClick =
                        onClickSettingsMenu(
                            menu = menu,
                            onAccountClick = onAccountClick,
                            onInformationClick = onInformationClick,
                            onNotificationSettingsClick = onNotificationSettingsClick,
                            onReportClick = onReportClick,
                            onHiddenShopsClick = onHiddenShopsClick,
                            onSubscribedShopsClick = onSubscribedShopsClick,
                            onBookmarkedShopsClick = onBookmarkedShopsClick,
                        ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
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
        modifier =
            Modifier
                .fillMaxWidth()
                .noRippleClickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

private fun settingsMenuTitle(menu: SettingsMenu): StringResource =
    when (menu) {
        SettingsMenu.ACCOUNT -> Res.string.settings_account_menu
        SettingsMenu.INFORMATION -> Res.string.settings_information_menu
        SettingsMenu.NOTIFICATION -> Res.string.settings_notification_menu
        SettingsMenu.REPORT -> Res.string.settings_report_menu
        SettingsMenu.HIDDEN_SHOPS -> Res.string.settings_hidden_shops_menu
        SettingsMenu.SUBSCRIBED_SHOPS -> Res.string.settings_subscribed_shops_menu
        SettingsMenu.BOOKMARKED_SHOPS -> Res.string.settings_bookmarked_shops_menu
    }

private fun onClickSettingsMenu(
    menu: SettingsMenu,
    onAccountClick: () -> Unit,
    onInformationClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onReportClick: () -> Unit,
    onHiddenShopsClick: () -> Unit,
    onSubscribedShopsClick: () -> Unit,
    onBookmarkedShopsClick: () -> Unit,
): () -> Unit =
    when (menu) {
        SettingsMenu.ACCOUNT -> onAccountClick
        SettingsMenu.INFORMATION -> onInformationClick
        SettingsMenu.NOTIFICATION -> onNotificationSettingsClick
        SettingsMenu.REPORT -> onReportClick
        SettingsMenu.HIDDEN_SHOPS -> onHiddenShopsClick
        SettingsMenu.SUBSCRIBED_SHOPS -> onSubscribedShopsClick
        SettingsMenu.BOOKMARKED_SHOPS -> onBookmarkedShopsClick
    }
