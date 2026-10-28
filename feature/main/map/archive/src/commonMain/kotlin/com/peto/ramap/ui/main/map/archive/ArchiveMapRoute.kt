package com.peto.ramap.ui.main.map.archive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.peto.ramap.designsystem.button.MapModeSwitchFab
import com.peto.ramap.designsystem.button.RetryButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.map.archive.component.ArchiveMapCanvas
import com.peto.ramap.ui.main.map.archive.component.ArchiveMapClusterPopup
import com.peto.ramap.ui.main.map.archive.component.ArchiveMapEmptyContent
import com.peto.ramap.ui.main.map.archive.component.ArchiveMapLoadingContent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapSideEffect
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapUiState
import com.peto.ramap.ui.main.map.archive.model.ArchiveLoadingStage
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapClusteringConfig
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.current_location_timeout_message
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.visited_shops_error

@Composable
fun ArchiveMapRoute(
    isBackEnabled: Boolean,
    onReturnToMap: () -> Unit,
    onFindVisitedShop: () -> Unit,
    viewModel: ArchiveMapViewModel,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is ArchiveMapSideEffect.ShowToast -> toastManager.show(effect.data)
        }
    }
    LaunchedEffect(viewModel) { viewModel.dispatch(ArchiveMapIntent.Enter) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.dispatch(ArchiveMapIntent.Exit) }
    }
    ArchiveMapContent(
        state = state,
        isBackEnabled = isBackEnabled,
        onReturnToMap = onReturnToMap,
        onFindVisitedShop = onFindVisitedShop,
        onIntent = viewModel::dispatch,
        onLocationPermissionBlocked = {
            scope.launch {
                toastManager.show(
                    ToastData(
                        message = Res.string.location_permission_enable_message,
                        type = ToastType.DEFAULT,
                        action = ToastAction(Res.string.location_permission_settings_action, appSettingsOpener::open),
                    ),
                )
            }
        },
        onCurrentLocationTimeout = {
            scope.launch { toastManager.show(ToastData(Res.string.current_location_timeout_message, ToastType.DEFAULT)) }
        },
    )
}

@Composable
internal fun ArchiveMapContent(
    state: ArchiveMapUiState,
    isBackEnabled: Boolean,
    onReturnToMap: () -> Unit,
    onFindVisitedShop: () -> Unit,
    onIntent: (ArchiveMapIntent) -> Unit,
    onLocationPermissionBlocked: () -> Unit,
    onCurrentLocationTimeout: () -> Unit,
) {
    val isPreview = LocalInspectionMode.current
    var overlappingShops by remember { mutableStateOf(emptyList<RamenShop>()) }
    var clusterOffset by remember { mutableStateOf<IntOffset?>(null) }
    val hasMapShops = state.hasLoaded && state.shops.isNotEmpty()
    val isEmpty = state.hasLoaded && state.shops.isEmpty() && !state.hasError
    var isMapReady by remember(state.userId, hasMapShops) { mutableStateOf(isPreview && hasMapShops) }
    var areInitialMarkersReady by remember(state.userId, hasMapShops) { mutableStateOf(false) }
    val loadingStage =
        when {
            !hasMapShops -> ArchiveLoadingStage.VISITS
            !areInitialMarkersReady -> ArchiveLoadingStage.PHOTOS
            else -> ArchiveLoadingStage.MAP
        }
    NavigationBackHandler(
        state = rememberNavigationEventState<NavigationEventInfo>(currentInfo = NavigationEventInfo.None),
        isBackEnabled = isBackEnabled && state.selectedShopId == null,
        onBackCompleted = {
            if (overlappingShops.isNotEmpty()) overlappingShops = emptyList() else onReturnToMap()
        },
    )
    Box(modifier = Modifier.fillMaxSize().background(CommonColor.White)) {
        if (hasMapShops) {
            ArchiveMapCanvas(
                shops = state.shops,
                photos = state.photos,
                failedPhotoReferences = state.photoState.failedMarkerSignatures,
                clusteringOptions = ArchiveMapClusteringConfig.options,
                selectedShopId = state.selectedShopId,
                cameraPosition = state.cameraPosition,
                shouldFocusVisits = state.shouldFocusVisits,
                onFocusConsumed = { onIntent(ArchiveMapIntent.FocusConsumed) },
                onCameraChanged = { onIntent(ArchiveMapIntent.CameraChanged(it)) },
                onShopClick = { onIntent(ArchiveMapIntent.SelectShop(it.id)) },
                onClusterClick = { shops, offset ->
                    overlappingShops = shops
                    clusterOffset = offset
                },
                onMapMoveStarted = { overlappingShops = emptyList() },
                onPhotoDownloadFailed = { id, reference -> onIntent(ArchiveMapIntent.PhotoDownloadFailed(id, reference)) },
                onLocationPermissionBlocked = onLocationPermissionBlocked,
                onCurrentLocationTimeout = onCurrentLocationTimeout,
                onInitialMarkersReady = { areInitialMarkersReady = true },
                onMapReady = { isMapReady = true },
            )
        }
        if (!isMapReady) {
            Box(modifier = Modifier.fillMaxSize().background(CommonColor.White))
        }
        when {
            state.hasError ->
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppText(text = stringResource(Res.string.visited_shops_error), style = AppTextStyle.B1, color = GrayColor.C500)
                    RetryButton(onClick = { onIntent(ArchiveMapIntent.Retry) })
                }

            isEmpty -> ArchiveMapEmptyContent(onFindVisitedShop = onFindVisitedShop, modifier = Modifier.fillMaxSize())
        }
        AnimatedVisibility(
            visible = !isMapReady && !state.hasError && !isEmpty,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = 240)),
        ) {
            ArchiveMapLoadingContent(stage = loadingStage, modifier = Modifier.fillMaxSize())
        }
        MapModeSwitchFab(
            isVisitedMode = true,
            onClick = onReturnToMap,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 84.dp),
        )
        if (overlappingShops.isNotEmpty()) {
            ArchiveMapClusterPopup(
                shops = overlappingShops,
                offset = clusterOffset,
                onShopClick = { shop ->
                    overlappingShops = emptyList()
                    onIntent(ArchiveMapIntent.SelectShop(shop.id))
                },
                onDismiss = { overlappingShops = emptyList() },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveMapContentEmptyPreview() {
    RamapTheme {
        ArchiveMapContent(
            state =
                ArchiveMapUiState(
                    visits = ArchiveShops(emptyList(), RamenShops(emptyMap())),
                    hasLoaded = true,
                ),
            isBackEnabled = true,
            onReturnToMap = {},
            onFindVisitedShop = {},
            onIntent = {},
            onLocationPermissionBlocked = {},
            onCurrentLocationTimeout = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveMapContentLoadingPreview() {
    RamapTheme {
        ArchiveMapContent(
            state = ArchiveMapUiState(hasLoaded = false),
            isBackEnabled = true,
            onReturnToMap = {},
            onFindVisitedShop = {},
            onIntent = {},
            onLocationPermissionBlocked = {},
            onCurrentLocationTimeout = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveMapContentErrorPreview() {
    RamapTheme {
        ArchiveMapContent(
            state =
                ArchiveMapUiState(
                    hasLoaded = false,
                    hasError = true,
                ),
            isBackEnabled = true,
            onReturnToMap = {},
            onFindVisitedShop = {},
            onIntent = {},
            onLocationPermissionBlocked = {},
            onCurrentLocationTimeout = {},
        )
    }
}
