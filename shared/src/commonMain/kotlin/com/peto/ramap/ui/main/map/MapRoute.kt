package com.peto.ramap.ui.main.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.core.base.ObserveAsEvents
import com.peto.ramap.designsystem.dialog.LoginGuideDialog
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBookmarkedShopsToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnBoundsChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnCategoryFilterToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnHiddenToggled
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialLocationFocusConsumed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnInitialMapRetryClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnKakaoLoginClicked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnLocationPermissionBlocked
import com.peto.ramap.ui.main.map.contract.MapIntent.OnMyLocationChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnQueryChanged
import com.peto.ramap.ui.main.map.contract.MapIntent.OnSearchResultsDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopDetailDismissed
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopReportSubmitted
import com.peto.ramap.ui.main.map.contract.MapIntent.OnShopSelected
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowLoginGuide
import com.peto.ramap.ui.main.map.contract.MapSideEffect.ShowToast
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapRoute(
    isBackEnabled: Boolean = true,
    onEventNavigate: (ShopEvent) -> Unit = {},
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    viewModel: MapViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLoginGuideDialog by remember { mutableStateOf(false) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            ShowLoginGuide -> showLoginGuideDialog = true
            is ShowToast ->
                toastManager.show(
                    sideEffect.data.copy(
                        action = sideEffect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
        }
    }

    MapContent(
        uiState = uiState,
        isBackEnabled = isBackEnabled,
        onBoundsChanged = { viewModel.dispatch(OnBoundsChanged(it)) },
        onMyLocationChanged = { viewModel.dispatch(OnMyLocationChanged(it)) },
        onLocationPermissionBlocked = { viewModel.dispatch(OnLocationPermissionBlocked) },
        onShopSelected = { shop, shouldFocus -> viewModel.dispatch(OnShopSelected(shop, shouldFocus)) },
        onShopDetailDismissed = { viewModel.dispatch(OnShopDetailDismissed) },
        onQueryChanged = { viewModel.dispatch(OnQueryChanged(it)) },
        onSearchResultsDismissed = { viewModel.dispatch(OnSearchResultsDismissed) },
        onInitialMapRetry = { viewModel.dispatch(OnInitialMapRetryClicked) },
        onInitialLocationFocusConsumed = { viewModel.dispatch(OnInitialLocationFocusConsumed) },
        onCategoryFilterToggled = { viewModel.dispatch(OnCategoryFilterToggled(it)) },
        onBookmarkToggled = { viewModel.dispatch(OnBookmarkToggled(it)) },
        onHiddenToggled = { viewModel.dispatch(OnHiddenToggled(it)) },
        onReportSubmit = { wrongFields, description ->
            viewModel.dispatch(OnShopReportSubmitted(wrongFields, description))
        },
        onBookmarkedShopsToggle = { viewModel.dispatch(OnBookmarkedShopsToggled) },
        onEventClick = onEventNavigate,
    )

    LoginGuideDialog(
        visible = showLoginGuideDialog,
        onDismiss = { showLoginGuideDialog = false },
        onConfirm = {
            showLoginGuideDialog = false
            viewModel.dispatch(OnKakaoLoginClicked)
        },
    )
}
