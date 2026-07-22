package com.peto.ramap.ui.bookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListIntent
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListSideEffect
import com.peto.ramap.ui.bookmark.contract.BookmarkedShopListUiState
import com.peto.ramap.ui.extension.stringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmark_removal_confirm_action
import ramap.shared.generated.resources.bookmark_removal_confirm_title
import ramap.shared.generated.resources.bookmarked_shops_empty_title
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_removal_dismiss_action
import ramap.shared.generated.resources.settings_bookmarked_shops_menu

@Composable
fun BookmarkedShopListRoute(
    onBack: () -> Unit,
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
        onShopClick = { removalTargetShopId = it.id },
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
fun BookmarkedShopListScreen(
    uiState: BookmarkedShopListUiState,
    removalTargetShopId: String?,
    onBack: () -> Unit,
    onShopClick: (RamenShop) -> Unit,
    onRemovalDismiss: () -> Unit,
    onRemovalConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CommonTopBar(
            title = stringResource(Res.string.settings_bookmarked_shops_menu),
            left = {
                Image(
                    painterResource(Res.drawable.ic_arrow3_left),
                    stringResource(Res.string.navigation_back),
                    Modifier.padding(18.dp).size(24.dp).noRippleClickable(onClick = onBack),
                )
            },
        )
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.showError ->
                    LoadErrorContent(
                        Res.drawable.laduck_error_confused,
                        stringResource(Res.string.settings_bookmarked_shops_menu),
                        stringResource(Res.string.data_load_failure_message),
                    )

                uiState.isOnlyLoading -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

                else -> BookmarkedShopListContent(uiState, onShopClick)
            }
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
}

@Composable
private fun BookmarkedShopListContent(
    uiState: BookmarkedShopListUiState,
    onShopClick: (RamenShop) -> Unit,
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
                onShopClick = onShopClick,
                categoryLabel = { stringResource(it.stringResource) },
                itemModifier = {
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .border(1.dp, GrayColor.C200, RoundedCornerShape(16.dp))
                },
                showDividers = false,
            )
        }

        if (uiState.isOverlayLoading) {
            RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        }
    }
}
