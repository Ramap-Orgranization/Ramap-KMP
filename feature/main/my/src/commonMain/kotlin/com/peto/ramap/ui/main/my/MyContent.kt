package com.peto.ramap.ui.main.my

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.my.component.LoginButton
import com.peto.ramap.ui.main.my.contract.MyTabUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_menu
import ramap.shared.generated.resources.ic_refresh
import ramap.shared.generated.resources.logout_menu
import ramap.shared.generated.resources.my_tab_account_section
import ramap.shared.generated.resources.my_tab_report_section
import ramap.shared.generated.resources.place_report_action
import ramap.shared.generated.resources.place_report_description
import ramap.shared.generated.resources.place_report_location_address
import ramap.shared.generated.resources.place_report_location_address_failure
import ramap.shared.generated.resources.place_report_location_empty
import ramap.shared.generated.resources.place_report_location_refresh
import ramap.shared.generated.resources.place_report_location_section_title
import ramap.shared.generated.resources.place_report_placeholder
import ramap.shared.generated.resources.place_report_url_section_title
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.settings_notification_menu

@Composable
fun MyContent(
    uiState: MyTabUiState,
    onKakaoLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAccountDeleteClick: () -> Unit,
    onHiddenShopsClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onPlaceUrlChanged: (String) -> Unit,
    onPlaceReportSubmit: () -> Unit,
    onLocationReportSubmit: () -> Unit,
    onCurrentAddressRefresh: () -> Unit,
) {
    val currentAddressText =
        when {
            uiState.currentAddress != null ->
                stringResource(Res.string.place_report_location_address, uiState.currentAddress)

            uiState.currentLocation != null -> stringResource(Res.string.place_report_location_address_failure)
            else -> stringResource(Res.string.place_report_location_empty)
        }
    val addressRefreshContentDescription = stringResource(Res.string.place_report_location_refresh)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                ).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(1.dp))

        if (!uiState.isLoggedIn) {
            SectionCard(
                title = stringResource(Res.string.my_tab_account_section),
            ) {
                LoginButton(
                    type = LoginType.KAKAO,
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    onClickLogin = { onKakaoLoginClick() },
                )
            }
        } else {
            SectionCard(
                title = uiState.accountLabel.orEmpty(),
            ) {
                AppButton(
                    text = stringResource(Res.string.logout_menu),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    backgroundColor = CommonColor.White,
                    textColor = GrayColor.C500,
                    border = BorderStroke(1.dp, GrayColor.C200),
                    onClick = onLogoutClick,
                )
                AppButton(
                    text = stringResource(Res.string.account_delete_menu),
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    backgroundColor = CommonColor.White,
                    textColor = GrayColor.C500,
                    border = BorderStroke(1.dp, GrayColor.C200),
                    onClick = onAccountDeleteClick,
                )

                AppButton(
                    text = stringResource(Res.string.settings_hidden_shops_menu),
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    onClick = onHiddenShopsClick,
                )
                AppButton(
                    text = stringResource(Res.string.settings_notification_menu),
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    onClick = onNotificationSettingsClick,
                )
            }
        }

        SectionCard(
            title = stringResource(Res.string.my_tab_report_section),
        ) {
            AppText(
                text = stringResource(Res.string.place_report_url_section_title),
                modifier = Modifier.padding(top = 16.dp),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            AppText(
                text = stringResource(Res.string.place_report_description),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
            TextField(
                value = uiState.placeUrl,
                onValueChange = onPlaceUrlChanged,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.place_report_placeholder),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                },
                minLines = 4,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = GrayColor.C050,
                        unfocusedContainerColor = GrayColor.C050,
                        disabledContainerColor = GrayColor.C050,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GrayColor.C400,
                    ),
            )
            AppButton(
                text = stringResource(Res.string.place_report_action),
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                enabled = uiState.canSubmitPlaceUrl,
                backgroundColor = if (uiState.canSubmitPlaceUrl) GrayColor.C500 else GrayColor.C200,
                onClick = onPlaceReportSubmit,
            )

            AppText(
                text = stringResource(Res.string.place_report_location_section_title),
                modifier = Modifier.padding(top = 20.dp),
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = currentAddressText,
                    modifier = Modifier.weight(1f),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
                IconButton(
                    modifier = Modifier.semantics { contentDescription = addressRefreshContentDescription },
                    enabled = uiState.currentLocation != null && !uiState.isAddressRefreshing,
                    onClick = onCurrentAddressRefresh,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_refresh),
                        contentDescription = null,
                        tint = if (uiState.currentLocation != null && !uiState.isAddressRefreshing) GrayColor.C500 else GrayColor.C300,
                    )
                }
            }
            AppButton(
                text = stringResource(Res.string.place_report_action),
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                enabled = uiState.currentLocation != null,
                backgroundColor = if (uiState.currentLocation != null) GrayColor.C500 else GrayColor.C200,
                onClick = onLocationReportSubmit,
            )
        }

        Spacer(modifier = Modifier.height(1.dp))
    }
}
