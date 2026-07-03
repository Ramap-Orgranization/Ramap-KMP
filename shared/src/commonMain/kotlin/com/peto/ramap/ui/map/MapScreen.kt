package com.peto.ramap.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.domain.model.Category
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.map.component.MenuCategoryFilterRow
import com.peto.ramap.ui.map.component.RamenShopDetailContent
import com.peto.ramap.ui.map.component.RamenShopSearchBar
import com.peto.ramap.ui.map.component.RamenShopSearchResultList
import com.peto.ramap.ui.map.contract.MapIntent
import com.peto.ramap.ui.map.contract.MapUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.location_permission_enable_message
import ramap.shared.generated.resources.location_permission_settings_action

@Composable
fun MapRoute(viewModel: MapViewModel = koinInject()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MapScreen(
        uiState = uiState,
        onBoundsChanged = { bounds ->
            viewModel.dispatch(MapIntent.OnBoundsChanged(bounds))
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
    )
}

@Composable
private fun MapScreen(
    uiState: MapUiState,
    onBoundsChanged: (MapBounds) -> Unit,
    onShopSelected: (RamenShop) -> Unit,
    onShopDetailDismissed: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearchResultsDismissed: () -> Unit,
    onCategoryFilterToggled: (Category) -> Unit,
) {
    val selectedShop: RamenShop? = uiState.selectedShop
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    var wasImeVisible by remember { mutableStateOf(false) }
    var myLocationRequestKey by remember { mutableStateOf(0) }
    var locationSettingsRequestKey by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionEnableMessage =
        stringResource(Res.string.location_permission_enable_message)
    val locationPermissionSettingsAction =
        stringResource(Res.string.location_permission_settings_action)

    LaunchedEffect(isImeVisible) {
        if (wasImeVisible && !isImeVisible) {
            focusManager.clearFocus()
        }
        wasImeVisible = isImeVisible
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                shops = uiState.markerShops,
                focusShops = uiState.focusShops,
                bounds = uiState.bounds,
                clusterBounds = uiState.clusterBounds,
                myLocationRequestKey = myLocationRequestKey,
                locationSettingsRequestKey = locationSettingsRequestKey,
                onBoundsChanged = onBoundsChanged,
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
                            top =
                                WindowInsets.statusBars
                                    .asPaddingValues()
                                    .calculateTopPadding() + 16.dp,
                        ).padding(horizontal = 10.dp),
            ) {
                RamenShopSearchBar(
                    query = uiState.query,
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

            CommonBottomSheet(
                visible = uiState.showBottomSheet,
                onDismissRequest = {
                    if (selectedShop != null) {
                        onShopDetailDismissed()
                    } else {
                        onSearchResultsDismissed()
                    }
                },
                config = CommonBottomSheetConfig(),
                content = {
                    when {
                        selectedShop != null -> {
                            RamenShopDetailContent(
                                shop = selectedShop,
                                waitingSystem = uiState.shopWaiting[selectedShop.id],
                            )
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
        }
    }
}

@Composable
private fun MyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .size(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    clip = false,
                ).semantics {
                    contentDescription = "내 위치로 이동"
                },
        color = CommonColor.White,
        shape = CircleShape,
        onClick = onClick,
    ) {
        Canvas(modifier = Modifier.padding(12.dp)) {
            val strokeWidth = 2.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val color = GrayColor.C500

            drawCircle(
                color = color,
                radius = size.minDimension * 0.32f,
                style = Stroke(width = strokeWidth),
            )
            drawLine(
                color = color,
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height * 0.2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(center.x, size.height * 0.8f),
                end = Offset(center.x, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(0f, center.y),
                end = Offset(size.width * 0.2f, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(size.width * 0.8f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = strokeWidth,
                center = center,
            )
        }
    }
}
