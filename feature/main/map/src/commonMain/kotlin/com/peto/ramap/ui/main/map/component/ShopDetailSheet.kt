package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.RamenShopUiModel
import com.peto.ramap.ui.main.map.model.ShopDetailUiState
import com.peto.ramap.ui.resource.wating.toUiModel
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.map_shop_detail_error_title
import ramap.shared.generated.resources.ranking_show_shop_on_map

@Composable
internal fun ShopDetailSheet(
    uiState: MapUiState,
    visible: Boolean = true,
    isBackEnabled: Boolean,
    maxHeight: Dp,
    showRequestedLoadingInSheet: Boolean = false,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onShopNotificationToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
    onShopShareClick: (RamenShop) -> Unit,
    onShopMapLinkClick: (RamenShop, String) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onShowOnMap: ((String) -> Unit)? = null,
) {
    val selectedShop = uiState.selectedShop
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(selectedShop?.id) { mutableStateOf(false) }
    val shouldShowMainSheet =
        selectedShop != null ||
            (showRequestedLoadingInSheet && uiState.isShopDetailLoading)

    if (visible && shouldShowMainSheet && !uiState.hasShopDetailLoadFailed) {
        CommonBottomSheet(
            visible = true,
            onDismissRequest = onDismiss,
            isBackEnabled = isBackEnabled,
            config = CommonBottomSheetConfig(maxHeight = maxHeight),
        ) {
            when (val detailState = uiState.shopDetailState) {
                is ShopDetailUiState.Loading ->
                    RamenLoadingIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                    )

                is ShopDetailUiState.Content -> {
                    val shop = detailState.detail.shop
                    RamenShopOverview(
                        shop = shop,
                        waitingSystem = uiState.shopWaiting[shop.id].toUiModel(),
                        isBookmarked = shop.id in uiState.bookmarkedShopIds,
                        isNotificationEnabled = shop.id in uiState.notificationShopIds,
                        isHidden = shop.id in uiState.hiddenShopIds,
                        onBookmarkClick = { onBookmarkToggled(shop) },
                        onNotificationClick = { onShopNotificationToggled(shop) },
                        onHiddenClick = {
                            if (uiState.isLoggedIn && shop.id !in uiState.hiddenShopIds) {
                                hideConfirmShop = shop
                            } else {
                                onHiddenToggled(shop)
                            }
                        },
                        onShareClick = { onShopShareClick(shop) },
                        onMapLinkClick = { provider -> onShopMapLinkClick(shop, provider) },
                        event = detailState.detail.event,
                        onEventClick = onEventClick,
                        onReportClick = { showReportDialog = true },
                    )
                    onShowOnMap?.let { showOnMap ->
                        AppButton(
                            text = stringResource(Res.string.ranking_show_shop_on_map),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp)
                                    .padding(horizontal = 20.dp),
                            onClick = { showOnMap(shop.id) },
                        )
                    }
                }

                ShopDetailUiState.Closed,
                is ShopDetailUiState.Error,
                -> Unit
            }
        }
    }

    if (visible && uiState.hasShopDetailLoadFailed) {
        CommonBottomSheet(
            visible = true,
            onDismissRequest = onDismiss,
            isBackEnabled = isBackEnabled,
            config = CommonBottomSheetConfig(maxHeight = maxHeight),
        ) {
            LoadErrorContent(
                image = Res.drawable.laduck_error_crying,
                title = stringResource(Res.string.map_shop_detail_error_title),
                description = stringResource(Res.string.map_shop_detail_error_description),
                onRetry = onRetry,
                modifier = Modifier.fillMaxWidth(),
                compact = true,
            )
        }
    }

    CommonDialog(
        visible = hideConfirmShop != null,
        confirmText = stringResource(Res.string.hide_shop_confirm_action),
        dismissText = stringResource(Res.string.hide_shop_confirm_dismiss),
        onDismissRequest = { hideConfirmShop = null },
        content = {
            AppText(
                text = stringResource(Res.string.hide_shop_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.hide_shop_confirm_description),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
        },
        onConfirm = {
            hideConfirmShop?.let(onHiddenToggled)
            hideConfirmShop = null
        },
        onDismiss = { hideConfirmShop = null },
    )

    selectedShop?.let { shop ->
        ReportDialog(
            shopUiModel =
                RamenShopUiModel(
                    shop = shop,
                    waitingVisible = uiState.shopWaiting[shop.id].toUiModel() != null,
                ),
            visible = showReportDialog,
            onDismissRequest = { showReportDialog = false },
            onSubmit = { wrongFields, description ->
                showReportDialog = false
                onReportSubmit(wrongFields, description)
            },
        )
    }
}
