package com.peto.ramap.designsystem.shop

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
import com.peto.ramap.designsystem.dialog.ReportDialog
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.shop.model.RamenShopUiModel
import com.peto.ramap.designsystem.shop.model.ShopDetailSheetUiState
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
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
fun ShopDetailContent(
    state: ShopDetailSheetUiState,
    isBackEnabled: Boolean,
    maxHeight: Dp,
    isNavigationBarPadded: Boolean = false,
    visible: Boolean = true,
    showRequestedLoadingInSheet: Boolean = false,
    waitingSystem: WaitingSystemUiModel? = null,
    isBookmarked: Boolean = false,
    isNotificationEnabled: Boolean = false,
    showNotificationActions: Boolean = true,
    isHidden: Boolean = false,
    isLoggedIn: Boolean = false,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onShopNotificationToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
    onShopShareClick: (RamenShop) -> Unit,
    onShopMapLinkClick: (RamenShop, String) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onShowOnMap: ((String) -> Unit)? = null,
    onPhoneClick: (String) -> Unit = {},
    onWaitingClick: (String) -> Unit = {},
    onExternalLinkClick: (String) -> Unit = {},
    isAppleMapsAvailable: Boolean = false,
    onAppleMapsClick: (RamenShop) -> Unit = {},
) {
    val selectedShop =
        when (state) {
            ShopDetailSheetUiState.Closed -> null
            is ShopDetailSheetUiState.Loading -> state.shop
            is ShopDetailSheetUiState.Content -> state.detail.shop
            is ShopDetailSheetUiState.Error -> state.shop
        }
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(selectedShop?.id) { mutableStateOf(false) }
    val shouldShowMainSheet =
        selectedShop != null ||
            (showRequestedLoadingInSheet && state is ShopDetailSheetUiState.Loading)

    if (visible && shouldShowMainSheet && state !is ShopDetailSheetUiState.Error) {
        CommonBottomSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            isBackEnabled = isBackEnabled,
            config =
                CommonBottomSheetConfig(
                    maxHeight = maxHeight,
                    isDraggable = true,
                    isContentDraggable = true,
                    isStatusBarPadded = true,
                    isNavigationBarPadded = isNavigationBarPadded,
                ),
        ) { dragModifier ->
            when (state) {
                is ShopDetailSheetUiState.Loading ->
                    RamenLoadingIndicator(
                        modifier =
                            dragModifier
                                .fillMaxWidth()
                                .heightIn(min = 240.dp),
                    )

                is ShopDetailSheetUiState.Content -> {
                    val shop = state.detail.shop
                    RamenShopOverview(
                        shop = shop,
                        likeCount = state.detail.likeCount,
                        dragAreaModifier = dragModifier,
                        waitingSystem = waitingSystem,
                        isBookmarked = isBookmarked,
                        isNotificationEnabled = isNotificationEnabled,
                        showNotificationActions = showNotificationActions,
                        isHidden = isHidden,
                        onBookmarkClick = { onBookmarkToggled(shop) },
                        onNotificationClick = { onShopNotificationToggled(shop) },
                        onHiddenClick = {
                            if (isLoggedIn && !isHidden) {
                                hideConfirmShop = shop
                            } else {
                                onHiddenToggled(shop)
                            }
                        },
                        onShareClick = { onShopShareClick(shop) },
                        onMapLinkClick = { provider -> onShopMapLinkClick(shop, provider) },
                        onPhoneClick = onPhoneClick,
                        onWaitingClick = onWaitingClick,
                        onExternalLinkClick = onExternalLinkClick,
                        isAppleMapsAvailable = isAppleMapsAvailable,
                        onAppleMapsClick = onAppleMapsClick,
                        event = state.detail.event,
                        onEventClick = onEventClick,
                        operatingNotice = state.detail.operatingNotice,
                        onOperatingNoticeClick = onOperatingNoticeClick,
                        menuSections = state.detail.menuSections,
                        menuUpdatedAt = state.detail.menuUpdatedAt,
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

                ShopDetailSheetUiState.Closed,
                is ShopDetailSheetUiState.Error,
                -> Unit
            }
        }
    }

    if (visible && state is ShopDetailSheetUiState.Error) {
        CommonBottomSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            isBackEnabled = isBackEnabled,
            config =
                CommonBottomSheetConfig(
                    maxHeight = maxHeight,
                    isDraggable = true,
                    isStatusBarPadded = true,
                    isNavigationBarPadded = isNavigationBarPadded,
                ),
        ) { _ ->
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
                    waitingVisible = waitingSystem != null,
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
