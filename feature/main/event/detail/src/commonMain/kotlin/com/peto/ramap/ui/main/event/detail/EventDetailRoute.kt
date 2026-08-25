package com.peto.ramap.ui.main.event.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.detail.component.EventDetailContent
import com.peto.ramap.ui.main.event.detail.component.EventNotificationButton
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnCollaboratorInstagramSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnCollaboratorShopSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnRetry
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnSourceLinkSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnVenueShopSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailIntent.OnWaitingLinkSelected
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.detail.contract.EventDetailSideEffect.ShowToast
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.event_detail_load_failure_title
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_permission_enable_message

@Composable
fun EventDetailRoute(
    eventId: String,
    onBack: () -> Unit,
    onUnavailable: () -> Unit,
    onShopClick: (String) -> Unit,
    onEventNavigate: (ShopEvent) -> Unit = {},
    shopDetailContent:
        @Composable (
            String,
            () -> Unit,
            (String) -> Unit,
            (ShopEvent) -> Unit,
        ) -> Unit = { _, _, _, _ -> },
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    viewModel: EventDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var selectedShopId by rememberSaveable(eventId) { mutableStateOf<String?>(null) }
    LaunchedEffect(eventId) {
        viewModel.dispatch(OnEntered(eventId))
    }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            EventUnavailable -> onUnavailable()
            is ShowToast ->
                toastManager.show(
                    ToastData(
                        message = sideEffect.message,
                        type = ToastType.ERROR,
                    ),
                )

            RequestNotificationPermission ->
                coroutineScope.launch {
                    if (requestNotificationPermission()) {
                        viewModel.dispatch(OnNotificationPermissionGranted)
                    } else {
                        toastManager.show(
                            ToastData(
                                message = Res.string.notification_permission_enable_message,
                                type = ToastType.DEFAULT,
                                action =
                                    ToastAction(
                                        label = Res.string.location_permission_settings_action,
                                        onClick = appSettingsOpener::open,
                                    ),
                            ),
                        )
                    }
                }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        EventDetailScreen(
            uiState = uiState,
            onBack = onBack,
            onVenueShopClick = { shopId ->
                viewModel.dispatch(OnVenueShopSelected(shopId))
                selectedShopId = shopId
            },
            onCollaboratorShopClick = { shopId ->
                viewModel.dispatch(OnCollaboratorShopSelected(shopId))
                selectedShopId = shopId
            },
            onCollaboratorInstagramClick = { url ->
                viewModel.dispatch(OnCollaboratorInstagramSelected)
                ExternalUriOpener.open(url)
            },
            onWaitingLinkClick = { url ->
                viewModel.dispatch(OnWaitingLinkSelected)
                ExternalUriOpener.open(url)
            },
            onSourceLinkClick = { url ->
                viewModel.dispatch(OnSourceLinkSelected)
                ExternalUriOpener.open(url)
            },
            onNotificationChanged = { enabled ->
                viewModel.dispatch(OnNotificationChanged(enabled))
            },
            onRetry = { viewModel.dispatch(OnRetry) },
        )

        selectedShopId?.let { shopId ->
            shopDetailContent(
                shopId,
                { selectedShopId = null },
                {
                    selectedShopId = null
                    onShopClick(it)
                },
                { event ->
                    selectedShopId = null
                    onEventNavigate(event)
                },
            )
        }
    }
}

@Composable
internal fun EventDetailScreen(
    uiState: EventDetailUiState,
    onBack: () -> Unit,
    onVenueShopClick: (String) -> Unit,
    onCollaboratorShopClick: (String) -> Unit,
    onCollaboratorInstagramClick: (String) -> Unit,
    onWaitingLinkClick: (String) -> Unit,
    onSourceLinkClick: (String) -> Unit,
    onNotificationChanged: (Boolean) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        topBar = {
            CommonTopBar(
                title =
                    stringResource(
                        uiState.event?.let { event -> ShopEventResourceMapper.detailTitle(event.type) }
                            ?: Res.string.event_detail_title,
                    ),
                left = {
                    Image(
                        painter = painterResource(Res.drawable.ic_arrow3_left),
                        contentDescription = stringResource(Res.string.navigation_back),
                        modifier =
                            Modifier
                                .padding(18.dp)
                                .size(24.dp)
                                .noRippleClickable(onClick = onBack),
                    )
                },
                right = {
                    uiState.event
                        ?.takeIf { event -> !event.isToday }
                        ?.let {
                            EventNotificationButton(
                                uiState = uiState,
                                onNotificationChanged = onNotificationChanged,
                            )
                        }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                uiState.event != null ->
                    EventDetailContent(
                        event = uiState.event,
                        hasCollaborators = uiState.hasCollaborators,
                        onVenueShopClick = onVenueShopClick,
                        onCollaboratorShopClick = onCollaboratorShopClick,
                        onCollaboratorInstagramClick = onCollaboratorInstagramClick,
                        onWaitingLinkClick = onWaitingLinkClick,
                        onSourceLinkClick = onSourceLinkClick,
                    )

                uiState.isEventLoading -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())

                uiState.hasEventLoadFailed ->
                    LoadErrorContent(
                        image = Res.drawable.laduck_error_crying,
                        title = stringResource(Res.string.event_detail_load_failure_title),
                        description = stringResource(Res.string.data_load_failure_message),
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailRoutePreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        EventDetailScreen(
            uiState = uiState,
            onBack = {},
            onVenueShopClick = {},
            onCollaboratorShopClick = {},
            onCollaboratorInstagramClick = {},
            onWaitingLinkClick = {},
            onSourceLinkClick = {},
            onNotificationChanged = {},
            onRetry = {},
        )
    }
}
