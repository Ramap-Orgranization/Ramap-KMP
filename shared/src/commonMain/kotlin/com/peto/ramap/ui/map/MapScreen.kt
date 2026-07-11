package com.peto.ramap.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.core.base.ObserveAsEvents
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.popup.CommonPopup
import com.peto.ramap.designsystem.popup.CommonPopupDivider
import com.peto.ramap.designsystem.popup.CommonPopupItem
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.map.component.MapCircleIconButton
import com.peto.ramap.ui.map.component.MenuCategoryFilterRow
import com.peto.ramap.ui.map.component.RamenShopDetailContent
import com.peto.ramap.ui.map.component.RamenShopSearchBar
import com.peto.ramap.ui.map.component.RamenShopSearchResultGuide
import com.peto.ramap.ui.map.component.RamenShopSearchResultList
import com.peto.ramap.ui.map.contract.MapUiState
import com.peto.ramap.ui.map.contract.OnAccountDeleteClicked
import com.peto.ramap.ui.map.contract.OnBookmarkToggled
import com.peto.ramap.ui.map.contract.OnBoundsChanged
import com.peto.ramap.ui.map.contract.OnCategoryFilterToggled
import com.peto.ramap.ui.map.contract.OnHiddenToggled
import com.peto.ramap.ui.map.contract.OnKakaoLoginClicked
import com.peto.ramap.ui.map.contract.OnLocationPermissionBlocked
import com.peto.ramap.ui.map.contract.OnLogoutClicked
import com.peto.ramap.ui.map.contract.OnMyLocationChanged
import com.peto.ramap.ui.map.contract.OnPersonalizationViewChanged
import com.peto.ramap.ui.map.contract.OnQueryChanged
import com.peto.ramap.ui.map.contract.OnSearchResultsDismissed
import com.peto.ramap.ui.map.contract.OnShopDetailDismissed
import com.peto.ramap.ui.map.contract.OnShopReportSubmitted
import com.peto.ramap.ui.map.contract.OnShopSelected
import com.peto.ramap.ui.map.contract.ShowLoginGuide
import com.peto.ramap.ui.map.contract.ShowToast
import com.peto.ramap.ui.map.model.MapPersonalization
import com.peto.ramap.ui.map.model.RamenShopUiModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_menu
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.login_required_action
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_dismiss
import ramap.shared.generated.resources.login_required_message
import ramap.shared.generated.resources.logout_menu
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_information_report_action
import ramap.shared.generated.resources.shop_information_report_description
import ramap.shared.generated.resources.shop_information_report_dismiss
import ramap.shared.generated.resources.shop_information_report_placeholder

@Composable
fun MapRoute(
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
                        action =
                            sideEffect.data.action?.copy(
                                onClick = appSettingsOpener::open,
                            ),
                    ),
                )
        }
    }

    MapScreen(
        uiState = uiState,
        showLoginGuideDialog = showLoginGuideDialog,
        onBoundsChanged = { bounds ->
            viewModel.dispatch(OnBoundsChanged(bounds))
        },
        onMyLocationChanged = { location ->
            viewModel.dispatch(OnMyLocationChanged(location))
        },
        onLocationPermissionBlocked = {
            viewModel.dispatch(OnLocationPermissionBlocked)
        },
        onShopSelected = { shop ->
            viewModel.dispatch(OnShopSelected(shop))
        },
        onShopDetailDismissed = {
            viewModel.dispatch(OnShopDetailDismissed)
        },
        onQueryChanged = { query ->
            viewModel.dispatch(OnQueryChanged(query))
        },
        onSearchResultsDismissed = {
            viewModel.dispatch(OnSearchResultsDismissed)
        },
        onCategoryFilterToggled = { category ->
            viewModel.dispatch(OnCategoryFilterToggled(category))
        },
        onBookmarkToggled = { shop ->
            viewModel.dispatch(OnBookmarkToggled(shop))
        },
        onHiddenToggled = { shop ->
            viewModel.dispatch(OnHiddenToggled(shop))
        },
        onReportSubmit = { wrongFields, description ->
            viewModel.dispatch(
                OnShopReportSubmitted(
                    wrongFields = wrongFields,
                    description = description,
                ),
            )
        },
        onPersonalizationViewChanged = { view ->
            viewModel.dispatch(OnPersonalizationViewChanged(view))
        },
        onKakaoLoginClick = {
            viewModel.dispatch(OnKakaoLoginClicked)
        },
        onLoginGuideDismiss = {
            showLoginGuideDialog = false
        },
        onLoginGuideConfirm = {
            showLoginGuideDialog = false
            viewModel.dispatch(OnKakaoLoginClicked)
        },
        onLogoutClick = {
            viewModel.dispatch(OnLogoutClicked)
        },
        onAccountDeleteClick = {
            viewModel.dispatch(OnAccountDeleteClicked)
        },
    )
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    showLoginGuideDialog: Boolean,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onShopSelected: (RamenShop) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
    onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    onPersonalizationViewChanged: (MapPersonalization) -> Unit,
    onKakaoLoginClick: () -> Unit,
    onLoginGuideDismiss: () -> Unit,
    onLoginGuideConfirm: () -> Unit,
    onLogoutClick: () -> Unit,
    onAccountDeleteClick: () -> Unit,
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
            isBackEnabled = selectedShop != null,
            onBackCompleted = onShopDetailDismissed,
        )

        RamapMapView(
            modifier = Modifier.fillMaxSize(),
            shops = uiState.markerShops,
            focusShops = uiState.focusShops,
            focusNearestToCurrentLocation = uiState.shouldFocusNearestSearchResult,
            focusRequestKey = uiState.focusRequestKey,
            selectedShopId = uiState.selectedShop?.id,
            onBoundsChanged = onBoundsChanged,
            onMyLocationChanged = onMyLocationChanged,
            onShopClick = onShopSelected,
            onLocationPermissionBlocked = onLocationPermissionBlocked,
        )

        Column(
            modifier =
                Modifier
                    .padding(
                        top = searchBarTopPadding,
                    ).padding(horizontal = 10.dp),
        ) {
            RamenShopSearchBar(
                query = uiState.search.input,
                onQueryChange = onQueryChanged,
            )

            MenuCategoryFilterRow(
                selectedCategories = uiState.filters,
                onCategoryClick = onCategoryFilterToggled,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        SettingsFab(
            isLoggedIn = uiState.isLoggedIn,
            accountLabel = uiState.accountLabel,
            isShowingBookmarkedShops = uiState.personalizationView == MapPersonalization.BOOKMARKED,
            isShowingHiddenShops = uiState.personalizationView == MapPersonalization.HIDDEN,
            onKakaoLoginClick = onKakaoLoginClick,
            onShowBookmarkedShopsClick = {
                onPersonalizationViewChanged(
                    if (uiState.personalizationView == MapPersonalization.BOOKMARKED) {
                        MapPersonalization.ALL
                    } else {
                        MapPersonalization.BOOKMARKED
                    },
                )
            },
            onShowHiddenShopsClick = {
                onPersonalizationViewChanged(
                    if (uiState.personalizationView == MapPersonalization.HIDDEN) {
                        MapPersonalization.ALL
                    } else {
                        MapPersonalization.HIDDEN
                    },
                )
            },
            onLogoutClick = onLogoutClick,
            onAccountDeleteClick = onAccountDeleteClick,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom =
                            WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding() + 24.dp,
                    ),
        )

        CommonBottomSheet(
            visible = uiState.showSearchResults,
            onDismissRequest = onSearchResultsDismissed,
            config = CommonBottomSheetConfig(),
            content = {
                val searchResultGuide = uiState.searchResultGuide
                when {
                    searchResultGuide != null -> {
                        RamenShopSearchResultGuide(guide = searchResultGuide)
                    }

                    uiState.showSearchResults -> {
                        RamenShopSearchResultList(
                            shops = uiState.searchResultShops,
                            onShopClick = onShopSelected,
                        )
                    }
                }
            },
        )

        selectedShop?.let { shop ->
            CommonBottomSheet(
                visible = true,
                onDismissRequest = onShopDetailDismissed,
                config = CommonBottomSheetConfig(maxHeight = detailBottomSheetMaxHeight),
                content = {
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
                    )
                },
            )
        }

        CommonDialog(
            visible = showLoginGuideDialog,
            confirmText = stringResource(Res.string.login_required_action),
            dismissText = stringResource(Res.string.login_required_dismiss),
            onDismissRequest = onLoginGuideDismiss,
            content = {
                AppText(
                    text = stringResource(Res.string.login_required_message),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                    textAlign = TextAlign.Center,
                )

                AppText(
                    text = stringResource(Res.string.login_required_description),
                    modifier = Modifier.padding(top = 8.dp),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                )
            },
            onConfirm = onLoginGuideConfirm,
            onDismiss = onLoginGuideDismiss,
        )

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
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                fieldOptions.forEach { option ->
                    ReportFieldCheckbox(
                        label = stringResource(option.label),
                        checked = option.field in selectedFields,
                        onCheckedChange = { checked ->
                            selectedFields =
                                if (checked) {
                                    selectedFields + option.field
                                } else {
                                    selectedFields - option.field
                                }
                        },
                    )
                }
            }

            TextField(
                value = description,
                onValueChange = { description = it },
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                placeholder = {
                    AppText(
                        text = stringResource(Res.string.shop_information_report_placeholder),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                },
                minLines = 3,
                maxLines = 5,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = GrayColor.C050,
                        unfocusedContainerColor = GrayColor.C050,
                        disabledContainerColor = GrayColor.C050,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = GrayColor.C400,
                    ),
            )
        },
        onConfirm = {
            if (canSubmit) {
                onSubmit(selectedFields, description)
            }
        },
        onDismiss = onDismissRequest,
    )
}

@Composable
private fun ReportFieldCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = GrayColor.C500,
                    uncheckedColor = GrayColor.C300,
                    checkmarkColor = CommonColor.White,
                ),
        )
        AppText(
            text = label,
            style = AppTextStyle.B2,
            color = GrayColor.C500,
        )
    }
}

@Composable
private fun SettingsFab(
    isLoggedIn: Boolean,
    accountLabel: String?,
    isShowingBookmarkedShops: Boolean,
    isShowingHiddenShops: Boolean,
    onKakaoLoginClick: () -> Unit,
    onShowBookmarkedShopsClick: () -> Unit,
    onShowHiddenShopsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAccountDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val popupOffset =
        remember(density, isLoggedIn) {
            with(density) {
                IntOffset(
                    x = (-145).dp.roundToPx(),
                    y =
                        if (isLoggedIn) {
                            (-208).dp.roundToPx()
                        } else {
                            (-20).dp.roundToPx()
                        },
                )
            }
        }

    Box(modifier = modifier) {
        val isActive = expanded || isShowingBookmarkedShops || isShowingHiddenShops

        MapCircleIconButton(
            isActive = isActive,
            onClick = { expanded = !expanded },
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_setting),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter =
                    ColorFilter.tint(
                        if (isActive) CommonColor.White else GrayColor.C500,
                    ),
            )
        }

        CommonPopup(
            visible = expanded,
            anchorOffset = popupOffset,
            onDismiss = { expanded = false },
        ) {
            Surface(
                modifier =
                    Modifier
                        .width(170.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                        ),
                shape = RoundedCornerShape(24.dp),
                color = CommonColor.White,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    accountLabel?.let {
                        AppText(
                            text = accountLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = AppTextStyle.B1,
                            color = GrayColor.C300,
                        )
                    }

                    CommonPopupDivider()

                    if (isLoggedIn) {
                        CommonPopupItem(
                            text = stringResource(Res.string.settings_bookmarked_shops_menu),
                            isSelected = isShowingBookmarkedShops,
                            onClick = {
                                expanded = false
                                onShowBookmarkedShopsClick()
                            },
                        )

                        CommonPopupDivider()

                        CommonPopupItem(
                            text = stringResource(Res.string.settings_hidden_shops_menu),
                            isSelected = isShowingHiddenShops,
                            onClick = {
                                expanded = false
                                onShowHiddenShopsClick()
                            },
                        )

                        CommonPopupDivider()

                        CommonPopupItem(
                            text = stringResource(Res.string.logout_menu),
                            onClick = {
                                expanded = false
                                onLogoutClick()
                            },
                        )

                        CommonPopupDivider()

                        CommonPopupItem(
                            text = stringResource(Res.string.account_delete_menu),
                            onClick = {
                                expanded = false
                                onAccountDeleteClick()
                            },
                        )
                    } else {
                        CommonPopupItem(
                            text = stringResource(Res.string.login_required_action),
                            onClick = {
                                expanded = false
                                onKakaoLoginClick()
                            },
                        )
                    }
                }
            }
        }
    }
}
