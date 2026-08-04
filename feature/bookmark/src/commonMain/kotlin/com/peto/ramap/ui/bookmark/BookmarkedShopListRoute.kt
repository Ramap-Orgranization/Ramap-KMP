package com.peto.ramap.ui.bookmark

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
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.resource.category.CategoryResourceMapper
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_confirm_action
import ramap.shared.generated.resources.bookmark_removal_confirm_title
import ramap.shared.generated.resources.bookmarked_shops_empty_title
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_bookmarked_shops_menu

@Composable
fun BookmarkedShopListRoute(
    onBack: () -> Unit,
    onShopOpen: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: BookmarkedShopListViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    var removalTargetShopId by remember { mutableStateOf<String?>(null) }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            is BookmarkedShopListSideEffect.ShowToast -> toastManager.show(sideEffect.data)
        }
    }

    BookmarkedShopListScreen(
        uiState = uiState,
        removalTargetShopId = removalTargetShopId,
        onBack = onBack,
        onShopOpen = { onShopOpen(it.id) },
        onRemovalRequested = { removalTargetShopId = it.id },
        onRetry = { viewModel.dispatch(BookmarkedShopListIntent.OnRetry) },
        onRemovalDismiss = { removalTargetShopId = null },
        onRemovalConfirm = {
            removalTargetShopId?.let { shopId ->
                viewModel.dispatch(BookmarkedShopListIntent.OnRemovalConfirmed(shopId))
            }
            removalTargetShopId = null
        },
    )
}

@Composable
internal fun BookmarkedShopListScreen(
    uiState: BookmarkedShopListUiState,
    removalTargetShopId: String?,
    onBack: () -> Unit,
    onShopOpen: (RamenShop) -> Unit,
    onRemovalRequested: (RamenShop) -> Unit,
    onRetry: () -> Unit,
    onRemovalDismiss: () -> Unit,
    onRemovalConfirm: () -> Unit,
) {
    SettingsListPage(
        title = Res.string.settings_bookmarked_shops_menu,
        onBack = onBack,
        showError = uiState.showError,
        showInitialLoading = uiState.isOnlyLoading,
        showOverlayLoading = uiState.isOverlayLoading,
        errorImage = Res.drawable.laduck_error_confused,
        errorDescription = Res.string.data_load_failure_message,
        onRetry = onRetry,
    ) {
        BookmarkedShopListContent(uiState, onShopOpen, onRemovalRequested)
    }

    CommonDialog(
        visible = removalTargetShopId != null,
        confirmText = stringResource(Res.string.bookmark_removal_confirm_action),
        dismissText = stringResource(Res.string.notification_removal_dismiss_action),
        onDismissRequest = onRemovalDismiss,
        content = {
            AppText(
                text = stringResource(Res.string.bookmark_removal_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = onRemovalConfirm,
        onDismiss = onRemovalDismiss,
    )
}

@Composable
private fun BookmarkedShopListContent(
    uiState: BookmarkedShopListUiState,
    onShopOpen: (RamenShop) -> Unit,
    onRemovalRequested: (RamenShop) -> Unit,
) {
    if (uiState.shops.isEmpty()) {
        ShopListEmptyContent(
            title = stringResource(Res.string.bookmarked_shops_empty_title),
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            RamenShopSearchResultList(
                shops = uiState.shops,
                onShopClick = onShopOpen,
                categoryLabel = { stringResource(CategoryResourceMapper.label(it)) },
                itemActionLabel = { stringResource(Res.string.bookmark_removal_confirm_action) },
                onItemAction = onRemovalRequested,
                itemModifier = {
                    Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                },
            )
        }
    }
}
