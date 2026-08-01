package com.peto.ramap.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LoginButton
import com.peto.ramap.designsystem.component.SettingsPage
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.auth.supportedLoginTypes
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.account.contract.AccountIntent
import com.peto.ramap.ui.account.contract.AccountSideEffect
import com.peto.ramap.ui.base.ObserveAsEvents
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_confirm_action
import ramap.shared.generated.resources.account_delete_confirm_description
import ramap.shared.generated.resources.account_delete_confirm_dismiss
import ramap.shared.generated.resources.account_delete_confirm_title
import ramap.shared.generated.resources.account_delete_menu
import ramap.shared.generated.resources.logout_menu
import ramap.shared.generated.resources.settings_account_menu

@Composable
fun AccountSettingsRoute(
    onBack: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: AccountViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isAccountDeleteConfirmDialogVisible by rememberSaveable { mutableStateOf(false) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is AccountSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    SettingsPage(
        title = Res.string.settings_account_menu,
        showLoading = uiState.loadState.isAnyLoading,
        onBack = onBack,
    ) {
        SectionCard(title = uiState.accountLabel) {
            if (uiState.isLoggedIn) {
                AppButton(
                    text = stringResource(Res.string.logout_menu),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .padding(horizontal = 20.dp),
                    backgroundColor = CommonColor.White,
                    textColor = GrayColor.C500,
                    border = BorderStroke(1.dp, GrayColor.C200),
                    onClick = { viewModel.dispatch(AccountIntent.OnLogoutClick) },
                )
                AppButton(
                    text = stringResource(Res.string.account_delete_menu),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .padding(horizontal = 20.dp),
                    backgroundColor = CommonColor.White,
                    textColor = GrayColor.C500,
                    border = BorderStroke(1.dp, GrayColor.C200),
                    onClick = { isAccountDeleteConfirmDialogVisible = true },
                )
            } else {
                supportedLoginTypes().forEachIndexed { index, loginType ->
                    LoginButton(
                        type = loginType,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = if (index == 0) 16.dp else 12.dp),
                        onClick = {
                            val intent =
                                when (loginType) {
                                    LoginType.KAKAO -> AccountIntent.OnKakaoLoginClick
                                    LoginType.APPLE -> AccountIntent.OnAppleLoginClick
                                }
                            viewModel.dispatch(intent)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))
        }
    }
    CommonDialog(
        visible = isAccountDeleteConfirmDialogVisible,
        confirmText = stringResource(Res.string.account_delete_confirm_action),
        dismissText = stringResource(Res.string.account_delete_confirm_dismiss),
        onDismissRequest = { isAccountDeleteConfirmDialogVisible = false },
        content = {
            AppText(
                text = stringResource(Res.string.account_delete_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.account_delete_confirm_description),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            isAccountDeleteConfirmDialogVisible = false
            viewModel.dispatch(AccountIntent.OnAccountDeleteConfirm)
        },
        onDismiss = { isAccountDeleteConfirmDialogVisible = false },
    )
}
