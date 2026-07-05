package com.peto.ramap.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.popup.CommonPopup
import com.peto.ramap.designsystem.popup.CommonPopupDivider
import com.peto.ramap.designsystem.popup.CommonPopupItem
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.map.component.MapCircleIconButton
import com.peto.ramap.ui.map.component.MenuCategoryFilterRow
import com.peto.ramap.ui.map.component.MyLocationButton
import com.peto.ramap.ui.map.component.RamenShopDetailContent
import com.peto.ramap.ui.map.component.RamenShopSearchBar
import com.peto.ramap.ui.map.component.RamenShopSearchResultGuide
import com.peto.ramap.ui.map.component.RamenShopSearchResultList
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapSideEffect
import com.peto.ramap.ui.map.contract.MapUiState
import com.peto.ramap.ui.map.model.MapPersonalization
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.account_delete_menu
import ramap.shared.generated.resources.hide_shop_confirm_action
import ramap.shared.generated.resources.hide_shop_confirm_description
import ramap.shared.generated.resources.hide_shop_confirm_dismiss
import ramap.shared.generated.resources.hide_shop_confirm_title
import ramap.shared.generated.resources.ic_setting
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.login_required_action
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_dismiss
import ramap.shared.generated.resources.login_required_message
import ramap.shared.generated.resources.logout_menu
import ramap.shared.generated.resources.settings_bookmarked_shops_menu
import ramap.shared.generated.resources.settings_hidden_shops_menu

@Composable
fun MapRoute(viewModel: MapViewModel = koinInject()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLoginGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                MapSideEffect.ShowLoginGuide -> showLoginGuideDialog = true

                is MapSideEffect.ShowToast ->
                    snackbarHostState.showSnackbar(
                        message = getString(sideEffect.messageResource),
                        duration = SnackbarDuration.Short,
                    )
            }
        }
    }

    MapScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showLoginGuideDialog = showLoginGuideDialog,
        onBoundsChanged = { bounds ->
            viewModel.dispatch(MapIntent.OnBoundsChanged(bounds))
        },
        onMyLocationChanged = { location ->
            viewModel.dispatch(MapIntent.OnMyLocationChanged(location))
        },
        onShopSelected = { shop ->
            viewModel.dispatch(MapIntent.OnShopSelected(shop))
        },
        onShopDetailDismissed = {
            viewModel.dispatch(MapIntent.OnShopDetailDismissed)
        },
        onQueryChanged = { query ->
            viewModel.dispatch(MapIntent.OnQueryChanged(query))
        },
        onSearchResultsDismissed = {
            viewModel.dispatch(MapIntent.OnSearchResultsDismissed)
        },
        onCategoryFilterToggled = { category ->
            viewModel.dispatch(MapIntent.OnCategoryFilterToggled(category))
        },
        onBookmarkToggled = { shop ->
            viewModel.dispatch(MapIntent.OnBookmarkToggled(shop))
        },
        onHiddenToggled = { shop ->
            viewModel.dispatch(MapIntent.OnHiddenToggled(shop))
        },
        onPersonalizationViewChanged = { view ->
            viewModel.dispatch(MapIntent.OnPersonalizationViewChanged(view))
        },
        onKakaoLoginClick = {
            viewModel.dispatch(MapIntent.OnKakaoLoginClicked)
        },
        onLoginGuideDismiss = {
            showLoginGuideDialog = false
        },
        onLoginGuideConfirm = {
            showLoginGuideDialog = false
            viewModel.dispatch(MapIntent.OnKakaoLoginClicked)
        },
        onLogoutClick = {
            viewModel.dispatch(MapIntent.OnLogoutClicked)
        },
        onAccountDeleteClick = {
            viewModel.dispatch(MapIntent.OnAccountDeleteClicked)
        },
    )
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    snackbarHostState: SnackbarHostState,
    showLoginGuideDialog: Boolean,
    onBoundsChanged: (MapBounds) -> Unit,
    onMyLocationChanged: (Location) -> Unit,
    onShopSelected: (RamenShop) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
    onBookmarkToggled: (RamenShop) -> Unit,
    onHiddenToggled: (RamenShop) -> Unit,
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
    var myLocationRequestKey by remember { mutableStateOf(0) }
    var locationSettingsRequestKey by remember { mutableStateOf(0) }
    var hideConfirmShop by remember { mutableStateOf<RamenShop?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionEnableMessage =
        stringResource(Res.string.location_permission_enable_message)
    val locationPermissionSettingsAction =
        stringResource(Res.string.location_permission_settings_action)
    val loginRequiredTitle = stringResource(Res.string.login_required_message)
    val loginRequiredDescription = stringResource(Res.string.login_required_description)
    val loginRequiredAction = stringResource(Res.string.login_required_action)
    val loginRequiredDismiss = stringResource(Res.string.login_required_dismiss)
    val hideShopConfirmTitle = stringResource(Res.string.hide_shop_confirm_title)
    val hideShopConfirmDescription = stringResource(Res.string.hide_shop_confirm_description)
    val hideShopConfirmAction = stringResource(Res.string.hide_shop_confirm_action)
    val hideShopConfirmDismiss = stringResource(Res.string.hide_shop_confirm_dismiss)
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

    LaunchedEffect(Unit) {
        myLocationRequestKey += 1
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val detailBottomSheetMaxHeight = maxHeight - searchBarTopPadding - searchBarHeight

            NavigationBackHandler(
                state = backEventState,
                isBackEnabled = selectedShop != null,
                onBackCompleted = onShopDetailDismissed,
            )

            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                shops = uiState.markerShops,
                focusShops = uiState.focusShops,
                focusNearestToCurrentLocation = uiState.shouldFocusNearestSearchResult,
                selectedShopId = uiState.selectedShop?.id,
                bounds = uiState.bounds,
                clusterBounds = uiState.clusterBounds,
                myLocationRequestKey = myLocationRequestKey,
                locationSettingsRequestKey = locationSettingsRequestKey,
                onBoundsChanged = onBoundsChanged,
                onMyLocationChanged = onMyLocationChanged,
                onShopClick = onShopSelected,
                onLocationPermissionBlocked = {
                    coroutineScope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = locationPermissionEnableMessage,
                                actionLabel = locationPermissionSettingsAction,
                                duration = SnackbarDuration.Short,
                            )

                        if (result == SnackbarResult.ActionPerformed) {
                            locationSettingsRequestKey += 1
                        }
                    }
                },
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

            MyLocationButton(
                onClick = { myLocationRequestKey += 1 },
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 16.dp,
                            bottom =
                                WindowInsets.navigationBars
                                    .asPaddingValues()
                                    .calculateBottomPadding() + 24.dp,
                        ),
            )

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
                        )
                    },
                )
            }

            CommonDialog(
                visible = showLoginGuideDialog,
                confirmText = loginRequiredAction,
                dismissText = loginRequiredDismiss,
                onDismissRequest = onLoginGuideDismiss,
                content = {
                    AppText(
                        text = loginRequiredTitle,
                        style = AppTextStyle.T1,
                        color = GrayColor.C500,
                        textAlign = TextAlign.Center,
                    )

                    AppText(
                        text = loginRequiredDescription,
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
                confirmText = hideShopConfirmAction,
                dismissText = hideShopConfirmDismiss,
                onDismissRequest = { hideConfirmShop = null },
                content = {
                    AppText(
                        text = hideShopConfirmTitle,
                        style = AppTextStyle.T1,
                        color = GrayColor.C500,
                        textAlign = TextAlign.Center,
                    )

                    AppText(
                        text = hideShopConfirmDescription,
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
        }
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
