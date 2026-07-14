package com.peto.ramap.ui.main.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.main.component.LaduckLoadingContent
import com.peto.ramap.ui.main.component.LoadErrorContent
import com.peto.ramap.ui.main.component.MapCircleIconButton
import com.peto.ramap.ui.main.component.MenuCategoryFilterRow
import com.peto.ramap.ui.main.component.RamenShopDetailContent
import com.peto.ramap.ui.main.component.RamenShopSearchBar
import com.peto.ramap.ui.main.component.RamenShopSearchResultGuide
import com.peto.ramap.ui.main.component.RamenShopSearchResultList
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.InitialMapLoadState
import com.peto.ramap.ui.main.map.model.MapPersonalization
import com.peto.ramap.ui.main.map.model.RamenShopUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.initial_map_error_description
import ramap.shared.generated.resources.initial_map_error_title
import ramap.shared.generated.resources.initial_map_loading_message
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_information_report_action
import ramap.shared.generated.resources.shop_information_report_description
import ramap.shared.generated.resources.shop_information_report_dismiss
import ramap.shared.generated.resources.shop_information_report_placeholder

@Composable
fun MapContent(
    uiState: MapUiState,
    isBackEnabled: Boolean,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onShopSelected: (RamenShop) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onInitialMapRetry: () -> Unit,
    onInitialLocationFocusConsumed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onBookmarkedShopsToggle: () -> Unit,
) {
    val selectedShop: RamenShop? = uiState.selectedShop
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var wasImeVisible by remember { mutableStateOf(false) }
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    var showReportDialog by remember(selectedShop?.id) { mutableStateOf(false) }

    val backEventState =
        rememberNavigationEventState<NavigationEventInfo>(
            currentInfo = NavigationEventInfo.None,
        )
    val searchBarTopPadding =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding() + 16.dp
    val searchBarHeight = 52.dp

    LaunchedEffect(isImeVisible) {
        if (wasImeVisible && !isImeVisible) {
            focusManager.clearFocus()
        }
        wasImeVisible = isImeVisible
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val detailBottomSheetMaxHeight = maxHeight - searchBarTopPadding - searchBarHeight

        NavigationBackHandler(
            state = backEventState,
            isBackEnabled = isBackEnabled && selectedShop != null,
            onBackCompleted = onShopDetailDismissed,
        )

        RamapMapView(
            modifier = Modifier.fillMaxSize(),
            shops = uiState.markerShops,
            focusShops = uiState.focusShops,
            focusNearestToCurrentLocation = uiState.shouldFocusNearestSearchResult,
            focusRequestKey = uiState.focusRequestKey,
            initialFocusLocation = uiState.initialFocusLocation,
            initialFocusRequestKey = uiState.initialFocusRequestKey,
            shouldBootstrapInitialLocationFocus = uiState.shouldBootstrapInitialLocationFocus,
            selectedShopId = uiState.selectedShop?.id,
            onMapMoveStarted = {
                if (isImeVisible) focusManager.clearFocus()
            },
            onBoundsChanged = onBoundsChanged,
            onInitialFocusConsumed = onInitialLocationFocusConsumed,
            onMyLocationChanged = onMyLocationChanged,
            onShopClick = onShopSelected,
            onLocationPermissionBlocked = onLocationPermissionBlocked,
        )

        Column(
            modifier =
                Modifier
                    .padding(top = searchBarTopPadding)
                    .padding(horizontal = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RamenShopSearchBar(
                    query = uiState.search.input,
                    onQueryChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                )

                BookmarkedFilterButton(
                    isActive = uiState.personalizationView == MapPersonalization.BOOKMARKED,
                    onClick = onBookmarkedShopsToggle,
                )
            }

            MenuCategoryFilterRow(
                selectedCategories = uiState.filters,
                onCategoryClick = onCategoryFilterToggled,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        CommonBottomSheet(
            visible = uiState.showSearchResults,
            onDismissRequest = onSearchResultsDismissed,
            isBackEnabled = isBackEnabled,
            config = CommonBottomSheetConfig(),
        ) {
            val searchResultGuide = uiState.searchResultGuide
            when {
                searchResultGuide != null -> RamenShopSearchResultGuide(guide = searchResultGuide)
                uiState.showSearchResults ->
                    RamenShopSearchResultList(
                        shops = uiState.searchResultShops,
                        onShopClick = onShopSelected,
                    )
            }
        }

        selectedShop?.let { shop ->
            CommonBottomSheet(
                visible = uiState.shopDetail != null,
                onDismissRequest = onShopDetailDismissed,
                isBackEnabled = isBackEnabled,
                config = CommonBottomSheetConfig(maxHeight = detailBottomSheetMaxHeight),
            ) {
                uiState.shopDetail?.let { detail ->
                    RamenShopDetailContent(
                        shop = shop,
                        waitingSystem = uiState.shopWaiting[shop.id],
                        isBookmarked = shop.id in uiState.bookmarkedShopIds,
                        isHidden = shop.id in uiState.hiddenShopIds,
                        onBookmarkClick = { onBookmarkToggled(shop) },
                        onHiddenClick = {
                            if (uiState.isLoggedIn && shop.id !in uiState.hiddenShopIds) {
                                hideConfirmShop = shop
                            } else {
                                onHiddenToggled(shop)
                            }
                        },
                        onReportClick = { showReportDialog = true },
                        event = detail.event,
                        onEventClick = onEventClick,
                    )
                }
            }

            if (uiState.isShopDetailLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GrayColor.C500,
                )
            }
        }

        if (uiState.initialMapLoadState != InitialMapLoadState.CONTENT) {
            Surface(modifier = Modifier.fillMaxSize(), color = CommonColor.White) {
                when (uiState.initialMapLoadState) {
                    InitialMapLoadState.LOADING ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            LaduckLoadingContent()
                            AppText(
                                text = stringResource(Res.string.initial_map_loading_message),
                                style = AppTextStyle.T1,
                                color = GrayColor.C500,
                            )
                        }

                    InitialMapLoadState.ERROR ->
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_crying,
                            title = stringResource(Res.string.initial_map_error_title),
                            description = stringResource(Res.string.initial_map_error_description),
                            onRetry = onInitialMapRetry,
                            modifier = Modifier.fillMaxSize(),
                        )

                    InitialMapLoadState.CONTENT -> Unit
                }
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

        selectedShop?.takeIf { showReportDialog }?.let { shop ->
            ShopInformationReportDialog(
                shopUiModel =
                    RamenShopUiModel(
                        shop = shop,
                        waitingVisible = uiState.shopWaiting[shop.id]?.providerUrl != null,
                    ),
                onDismissRequest = { showReportDialog = false },
                onSubmit = { wrongFields, description ->
                    showReportDialog = false
                    onReportSubmit(wrongFields, description)
                },
            )
        }
    }
}

@Composable
private fun BookmarkedFilterButton(
    isActive: Boolean,
    onClick: () -> Unit,
) {
    MapCircleIconButton(
        isActive = isActive,
        onClick = onClick,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_kid_star),
            contentDescription = stringResource(Res.string.bookmarked_shops_toggle),
            modifier = Modifier.size(22.dp),
            colorFilter =
                ColorFilter.tint(
                    if (isActive) CommonColor.White else GrayColor.C500,
                ),
        )
    }
}

@Composable
private fun ShopInformationReportDialog(
    shopUiModel: RamenShopUiModel,
    onDismissRequest: () -> Unit,
    onSubmit: (Set<ShopInformationField>, String) -> Unit,
) {
    val shop = shopUiModel.shop
    var selectedFields by remember(shop.id) { mutableStateOf(emptySet<ShopInformationField>()) }
    var description by remember(shop.id) { mutableStateOf("") }
    val fieldOptions = shopUiModel.reportFieldOptions
    val canSubmit = selectedFields.isNotEmpty() || description.isNotBlank()

    CommonDialog(
        visible = true,
        confirmText = stringResource(Res.string.shop_information_report_action),
        dismissText = stringResource(Res.string.shop_information_report_dismiss),
        confirmEnabled = canSubmit,
        onDismissRequest = onDismissRequest,
        content = {
            AppText(
                text = stringResource(Res.string.shop_detail_link_report),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.shop_information_report_description, shop.name),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                fieldOptions.forEach { option ->
                    Checkbox(
                        checked = option.field in selectedFields,
                        onCheckedChange = { checked ->
                            selectedFields =
                                if (checked) selectedFields + option.field else selectedFields - option.field
                        },
                        colors =
                            CheckboxDefaults.colors(
                                checkedColor = GrayColor.C500,
                                uncheckedColor = GrayColor.C300,
                                checkmarkColor = CommonColor.White,
                            ),
                    )
                    AppText(
                        text = stringResource(option.label),
                        style = AppTextStyle.B2,
                        color = GrayColor.C500,
                    )
                }
            }
            TextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.shop_information_report_placeholder),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                },
            )
        },
        onConfirm = {
            if (canSubmit) onSubmit(selectedFields, description)
        },
        onDismiss = onDismissRequest,
    )
}
