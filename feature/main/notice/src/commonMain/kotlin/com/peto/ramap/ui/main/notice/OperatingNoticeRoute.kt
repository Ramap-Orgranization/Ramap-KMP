package com.peto.ramap.ui.main.notice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeIntent
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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

    OperatingNoticeScreen(
        uiState = uiState,
        onBack = onBack,
        onEventListClick = onEventListClick,
        onRefresh = { viewModel.dispatch(OperatingNoticeIntent.OnRefreshed) },
        onRetry = { viewModel.dispatch(OperatingNoticeIntent.OnRetried) },
        onShopClick = onShopClick,
        isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
        onSourceClick = ExternalUriOpener::open,
    )
}
