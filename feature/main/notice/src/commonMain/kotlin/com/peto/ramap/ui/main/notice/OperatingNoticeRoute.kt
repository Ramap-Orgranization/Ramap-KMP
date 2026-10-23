package com.peto.ramap.ui.main.notice

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.login.LoginButton
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_saved_shops_description

@Composable
fun OperatingNoticeRoute(
    onBack: () -> Unit,
    onEventListClick: () -> Unit,
    onShopClick: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: OperatingNoticeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is OperatingNoticeSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    Box {
        OperatingNoticeScreen(
            uiState = uiState,
            onBack = onBack,
            onEventListClick = onEventListClick,
            onRefresh = { viewModel.dispatch(OperatingNoticeIntent.OnRefreshed) },
            onRetry = { viewModel.dispatch(OperatingNoticeIntent.OnRetried) },
            onShopClick = onShopClick,
            onQueryChanged = { viewModel.dispatch(OperatingNoticeIntent.OnQueryChanged(it)) },
            onSavedToggled = { viewModel.dispatch(OperatingNoticeIntent.OnSavedToggled) },
            onPersonalizationRetry = { viewModel.dispatch(OperatingNoticeIntent.OnPersonalizationRetried) },
            onNoticeClick = { viewModel.dispatch(OperatingNoticeIntent.OnNoticeClicked(it)) },
            isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
            onSourceClick = ExternalUriOpener::open,
        )

        LoginGuideDialog(
            visible = uiState.showLoginGuide,
            description = Res.string.login_required_saved_shops_description,
            onDismiss = { viewModel.dispatch(OperatingNoticeIntent.OnLoginDismissed) },
            onLoginTypeSelected = { viewModel.dispatch(OperatingNoticeIntent.OnLoginSelected(it)) },
            loginButton = { type, onClick -> LoginButton(type, onClick) },
        )
    }
}
