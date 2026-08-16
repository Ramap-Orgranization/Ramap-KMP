package com.peto.ramap.ui.main.event.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.main.event.detail.component.EventCancellationNotice
import com.peto.ramap.ui.main.event.detail.component.EventImages
import com.peto.ramap.ui.main.event.detail.component.EventNotificationButton
import com.peto.ramap.ui.main.event.detail.component.EventTag
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
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.event_content
import ramap.shared.generated.resources.event_date
import ramap.shared.generated.resources.event_detail_load_failure_title
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.event_instagram_action
import ramap.shared.generated.resources.event_store_renewal_venue
import ramap.shared.generated.resources.event_venue
import ramap.shared.generated.resources.event_waiting
import ramap.shared.generated.resources.event_waiting_action
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.instagram_icon
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
                title = stringResource(Res.string.event_detail_title),
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

@Composable
private fun EventDetailContent(
    event: ShopEvent,
    hasCollaborators: Boolean,
    onVenueShopClick: (String) -> Unit,
    onCollaboratorShopClick: (String) -> Unit,
    onCollaboratorInstagramClick: (String) -> Unit,
    onWaitingLinkClick: (String) -> Unit,
    onSourceLinkClick: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 5.dp),
        ) {
            EventTag(
                text = stringResource(ShopEventResourceMapper.dateLabel(event)),
                isStatus = ShopEventResourceMapper.statusLabel(event) != null,
            )
            EventTag(stringResource(ShopEventResourceMapper.typeLabel(event.type)))
        }
        AppText("🍜 ${event.title}", style = AppTextStyle.H1, color = GrayColor.C500)

        event.cancellationReason?.let { reason ->
            EventCancellationNotice(
                reason = reason,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        EventImages(event.displayImageUrls)

        SectionCard {
            Column(
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                EventInfoRow(
                    label =
                        stringResource(
                            if (event.type == ShopEventType.STORE_RENEWAL) {
                                Res.string.event_store_renewal_venue
                            } else {
                                Res.string.event_venue
                            },
                        ),
                    content = {
                        RamenShopSummary(
                            shop = event.venueShop,
                            onClick = { onVenueShopClick(event.venueShop.id) },
                            categoryLabel = { category ->
                                stringResource(CategoryResourceMapper.label(category))
                            },
                            modifier = Modifier.padding(vertical = 5.dp),
                        )
                    },
                )
                if (hasCollaborators) {
                    EventInfoRow(
                        label = stringResource(ShopEventResourceMapper.collaboratorLabel(event)),
                        content = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                event.collaboratorShops.forEach { shop ->
                                    RamenShopSummary(
                                        shop = shop,
                                        onClick = { onCollaboratorShopClick(shop.id) },
                                        categoryLabel = { category ->
                                            stringResource(CategoryResourceMapper.label(category))
                                        },
                                    )
                                }
                                event.externalParticipants.forEach { participant ->
                                    EventVenueLink(
                                        title = participant.name,
                                        modifier =
                                            Modifier
                                                .padding(bottom = 5.dp)
                                                .padding(horizontal = 15.dp),
                                        onClick = {
                                            onCollaboratorInstagramClick(participant.instagramUrl)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }

                EventInfoRow(
                    label = stringResource(Res.string.event_date),
                    content = {
                        EventValue(
                            value =
                                eventDateText(
                                    event.startDate,
                                    event.displayEndDate,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 5.dp),
                            textAlign = TextAlign.Center,
                        )
                    },
                )
            }
        }
        if (event.description.isNotBlank()) {
            SectionCard(title = stringResource(Res.string.event_content)) {
                EventValue(
                    value = event.description,
                    modifier =
                        Modifier
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                )
            }
        }
        event.waitingMethod?.let { waiting ->
            SectionCard(title = stringResource(Res.string.event_waiting)) {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(vertical = 10.dp)) {
                    EventValue(waiting)
                    event.waitingUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { url ->
                        AppButton(
                            text = stringResource(Res.string.event_waiting_action),
                            icon = Res.drawable.catchtable,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            textColor = CommonColor.Black,
                            backgroundColor = GrayColor.C100,
                            onClick = { onWaitingLinkClick(url) },
                        )
                    }
                }
            }
        }
        if (ExternalUriOpener.isSupportedWebUri(event.sourceUrl)) {
            AppButton(
                text = stringResource(Res.string.event_instagram_action),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(INSTAGRAM_GRADIENT, RoundedCornerShape(16.dp)),
                backgroundColor = Color.Transparent,
                onClick = {
                    onSourceLinkClick(event.sourceUrl)
                },
            )
        }
    }
}

@Composable
private fun EventInfoRow(
    label: String,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(GrayColor.C100),
            contentAlignment = Alignment.Center,
        ) {
            AppText(
                text = label,
                modifier = Modifier.padding(vertical = 4.dp),
                style = AppTextStyle.T3,
                color = GrayColor.C500,
            )
        }
        content()
    }
}

private val INSTAGRAM_GRADIENT =
    Brush.horizontalGradient(
        listOf(
            InstagramColor.Yellow,
            InstagramColor.Orange,
            InstagramColor.Pink,
            InstagramColor.Purple,
            InstagramColor.Blue,
        ),
    )

@Composable
private fun EventValue(
    value: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) = AppText(
    value,
    modifier = modifier,
    style = AppTextStyle.B2,
    color = GrayColor.C500,
    textAlign = textAlign,
)

@Composable
private fun EventVenueLink(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .then(Modifier.noRippleClickable(onClick = onClick)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.instagram_icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(5.dp))

        AppText(
            text = title,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
    }
}

@Preview(name = "이벤트 상세 전체 상태", showBackground = true)
@Composable
private fun EventDetailRoutePreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    EventDetailScreenPreview(uiState)
}

@Composable
private fun EventDetailScreenPreview(uiState: EventDetailUiState) {
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
