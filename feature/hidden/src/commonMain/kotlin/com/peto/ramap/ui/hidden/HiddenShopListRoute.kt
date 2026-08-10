package com.peto.ramap.ui.hidden

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.SettingsListPage
import com.peto.ramap.designsystem.component.ShopListCount
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.hidden.contract.HiddenShopListIntent
import com.peto.ramap.ui.hidden.contract.HiddenShopListSideEffect
import com.peto.ramap.ui.hidden.contract.HiddenShopListUiState
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.hidden_shops_empty_title
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.unhide_shop_confirm_action
import ramap.shared.generated.resources.unhide_shop_confirm_title

@Composable
fun HiddenShopListRoute(
    onBackClick: () -> Unit,
    onShopOpen: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: HiddenShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var unhideTargetShopId by remember { mutableStateOf<String?>(null) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is HiddenShopListSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    HiddenShopListScreen(
        uiState = uiState,
        onBack = onBackClick,
        onShopOpen = { onShopOpen(it.id) },
        onUnhideRequested = { unhideTargetShopId = it.id },
        onRetry = { viewModel.dispatch(HiddenShopListIntent.OnRetry) },
    )
    CommonDialog(
        visible = unhideTargetShopId != null,
        confirmText = stringResource(Res.string.unhide_shop_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = { unhideTargetShopId = null },
        content = {
            AppText(
                text = stringResource(Res.string.unhide_shop_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            unhideTargetShopId?.let { shopId ->
                viewModel.dispatch(HiddenShopListIntent.OnUnhideConfirmed(shopId))
            }
            unhideTargetShopId = null
        },
        onDismiss = { unhideTargetShopId = null },
    )
}

@Composable
internal fun HiddenShopListScreen(
    uiState: HiddenShopListUiState,
    onBack: () -> Unit,
    onShopOpen: (RamenShop) -> Unit,
    onUnhideRequested: (RamenShop) -> Unit,
    onRetry: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.settings_hidden_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        HiddenShopListContent(uiState, onShopOpen, onUnhideRequested)
    }
}

@Composable
private fun HiddenShopListContent(
    uiState: HiddenShopListUiState,
    onShopOpen: (RamenShop) -> Unit,
    onUnhideRequested: (RamenShop) -> Unit,
) {
    if (uiState.shops.isEmpty()) {
        ShopListEmptyContent(
            title = stringResource(Res.string.hidden_shops_empty_title),
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            ShopListCount(count = uiState.shops.size)
            RamenShopSearchResultList(
                shops = uiState.shops,
                onShopClick = onShopOpen,
                categoryLabel = { category -> stringResource(CategoryResourceMapper.label(category)) },
                itemActionLabel = { stringResource(Res.string.unhide_shop_confirm_action) },
                onItemAction = onUnhideRequested,
                itemModifier = {
                    Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                },
            )
        }
    }
}
