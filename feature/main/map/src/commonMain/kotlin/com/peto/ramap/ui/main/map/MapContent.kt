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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.RamenShopSearchResultList
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.place.PlaceSearchResult
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MapBounds
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.extension.stringResource
import com.peto.ramap.ui.main.map.component.MapCircleIconButton
import com.peto.ramap.ui.main.map.component.MenuCategoryFilterRow
import com.peto.ramap.ui.main.map.component.PlaceSearchResultList
import com.peto.ramap.ui.main.map.component.RamenShopDetailContent
import com.peto.ramap.ui.main.map.component.RamenShopSearchBar
import com.peto.ramap.ui.main.map.component.RamenShopSearchResultGuide
import com.peto.ramap.ui.main.map.contract.MapUiState
import com.peto.ramap.ui.main.map.model.CameraPosition
import com.peto.ramap.ui.main.map.model.RamenShopUiModel
import com.peto.ramap.ui.main.map.model.WaitingSystemUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.retry_action
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_information_report_action
import ramap.shared.generated.resources.shop_information_report_description
import ramap.shared.generated.resources.shop_information_report_dismiss
import ramap.shared.generated.resources.shop_information_report_placeholder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    uiState: MapUiState,
    isBackEnabled: Boolean,
    onBoundsChanged: (MapBounds) -> Unit,
    onCameraPositionChanged: (CameraPosition) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
    onShopSelected: (RamenShop, Boolean) -> Unit,
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onInitialLocationFocusConsumed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onViewportLoadRetry: () -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onShopNotificationToggled: (RamenShop) -> Unit,
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
    val searchResultSheetState = rememberModalBottomSheetState()

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
            placeFocusLocation = uiState.placeFocusLocation,
            placeFocusRequestKey = uiState.placeFocusRequestKey,
            shouldBootstrapInitialLocationFocus = uiState.shouldBootstrapLocationFocusStatus,
            selectedShopId = uiState.selectedShop?.id,
            cameraPosition = uiState.cameraPosition,
            onMapMoveStarted = {
                if (isImeVisible) focusManager.clearFocus()
            },
            onBoundsChanged = onBoundsChanged,
            onCameraPositionChanged = onCameraPositionChanged,
            onInitialFocusConsumed = onInitialLocationFocusConsumed,
            onMyLocationChanged = onMyLocationChanged,
            onShopClick = { onShopSelected(it, false) },
            onLocationPermissionBlocked = onLocationPermissionBlocked,
            onCurrentLocationTimeout = onCurrentLocationTimeout,
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
                    isActive = uiState.isBookmarkedView,
                    onClick = onBookmarkedShopsToggle,
                )
            }

            MenuCategoryFilterRow(
                selectedCategories = uiState.filters,
                onCategoryClick = onCategoryFilterToggled,
                modifier = Modifier.padding(top = 6.dp),
            )

            if (uiState.hasViewportLoadFailed) {
                AppButton(
                    text = stringResource(Res.string.retry_action),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    onClick = onViewportLoadRetry,
                )
            }
        }

        if (uiState.showSearchResults) {
            ModalBottomSheet(
                onDismissRequest = onSearchResultsDismissed,
                sheetState = searchResultSheetState,
                containerColor = CommonColor.White,
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val searchResultGuide = uiState.searchResultGuide
                    when {
                        uiState.placeSearchResults.isNotEmpty() ->
                            PlaceSearchResultList(
                                places = uiState.placeSearchResults,
                                onPlaceClick = onPlaceSelected,
                            )

                        searchResultGuide != null ->
                            RamenShopSearchResultGuide(guide = searchResultGuide)

                        else ->
                            RamenShopSearchResultList(
                                shops = uiState.searchResultShops,
                                categoryLabel = { category -> stringResource(category.stringResource) },
                                onShopClick = { onShopSelected(it, true) },
                            )
                    }
                }
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
                    val waitingSystemUiModel = WaitingSystemUiModel.from(uiState.shopWaiting[shop.id])
                    RamenShopDetailContent(
                        shop = shop,
                        waitingSystemUiModel = waitingSystemUiModel,
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
                        waitingVisible = WaitingSystemUiModel.from(uiState.shopWaiting[shop.id]) != null,
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
                    val isSelected = option.field in selectedFields
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = isSelected,
                                    role = Role.Checkbox,
                                    onValueChange = { checked ->
                                        selectedFields =
                                            if (checked) selectedFields + option.field else selectedFields - option.field
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
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
