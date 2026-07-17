package com.peto.ramap.ui.main.my

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteConfirm
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnAccountDeleteDismiss
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentAddressRefresh
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnCurrentLocationReportSubmit
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnHiddenShopsClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnKakaoLoginClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnLoginGuideDismiss
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnLogoutClick
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnPlaceReportSubmit
import com.peto.ramap.ui.main.my.contract.MyTabIntent.OnPlaceUrlChanged
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.NavigateToHiddenShops
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect.ShowMyToast
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_confirm_action
import ramap.shared.generated.resources.account_delete_confirm_description
import ramap.shared.generated.resources.account_delete_confirm_dismiss
import ramap.shared.generated.resources.account_delete_confirm_title

@Composable
fun MyTabRoute(
    onHiddenShopsNavigate: () -> Unit,
    onNotificationSettingsNavigate: () -> Unit,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    viewModel: MyTabViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is ShowMyToast ->
                toastManager.show(
                    sideEffect.data.copy(
                        action = sideEffect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
            NavigateToHiddenShops -> onHiddenShopsNavigate()
        }
    }

    MyContent(
        uiState = uiState,
        onKakaoLoginClick = { viewModel.dispatch(OnKakaoLoginClick) },
        onLogoutClick = { viewModel.dispatch(OnLogoutClick) },
        onAccountDeleteClick = { viewModel.dispatch(OnAccountDeleteClick) },
        onHiddenShopsClick = { viewModel.dispatch(OnHiddenShopsClick) },
        onNotificationSettingsClick = onNotificationSettingsNavigate,
        onPlaceUrlChanged = { viewModel.dispatch(OnPlaceUrlChanged(it)) },
        onPlaceReportSubmit = { viewModel.dispatch(OnPlaceReportSubmit) },
        onLocationReportSubmit = { viewModel.dispatch(OnCurrentLocationReportSubmit) },
        onCurrentAddressRefresh = { viewModel.dispatch(OnCurrentAddressRefresh) },
    )

    CommonDialog(
        visible = uiState.showAccountDeleteConfirmDialog,
        confirmText = stringResource(Res.string.account_delete_confirm_action),
        dismissText = stringResource(Res.string.account_delete_confirm_dismiss),
        confirmEnabled = !uiState.isDeletingAccount,
        onDismissRequest = { viewModel.dispatch(OnAccountDeleteDismiss) },
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
        onConfirm = { viewModel.dispatch(OnAccountDeleteConfirm) },
        onDismiss = { viewModel.dispatch(OnAccountDeleteDismiss) },
    )

    LoginGuideDialog(
        visible = uiState.showLoginGuideDialog,
        onDismiss = { viewModel.dispatch(OnLoginGuideDismiss) },
        onConfirm = { viewModel.dispatch(OnKakaoLoginClick) },
    )
}
