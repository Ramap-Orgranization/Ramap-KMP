package com.peto.ramap.ui.main.event

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
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
import com.peto.ramap.ui.component.eventDateText
import com.peto.ramap.ui.main.event.component.EventMapActions
import com.peto.ramap.ui.main.event.component.EventNotificationButton
import com.peto.ramap.ui.main.event.component.EventTag
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnCollaboratorInstagramSelected
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnCollaboratorShopSelected
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationPermissionGranted
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnSourceLinkSelected
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnVenueShopSelected
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnWaitingLinkSelected
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.RequestNotificationPermission
import com.peto.ramap.ui.main.event.contract.EventDetailUiState
import com.peto.ramap.ui.resource.event.ShopEventResourceMapper
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.event_content
import ramap.shared.generated.resources.event_date
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.event_instagram_action
import ramap.shared.generated.resources.event_venue
import ramap.shared.generated.resources.event_waiting
import ramap.shared.generated.resources.event_waiting_action
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.location_permission_settings_action
import ramap.shared.generated.resources.navigation_back
import ramap.shared.generated.resources.notification_permission_enable_message

@Composable
fun EventDetailRoute(
    eventId: String,
    onBack: () -> Unit,
    onUnavailable: () -> Unit,
    onShopClick: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
    viewModel: EventDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(eventId) {
        viewModel.dispatch(OnEntered(eventId))
    }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            EventUnavailable -> onUnavailable()
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
    EventDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onVenueShopClick = { shopId ->
            viewModel.dispatch(
                OnVenueShopSelected(shopId),
            )
            onShopClick(shopId)
        },
        onCollaboratorShopClick = { shopId ->
            viewModel.dispatch(
                OnCollaboratorShopSelected(shopId),
            )
            onShopClick(shopId)
        },
        onCollaboratorInstagramClick = { url ->
            viewModel.dispatch(
                OnCollaboratorInstagramSelected,
            )
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
            viewModel.dispatch(
                OnNotificationChanged(enabled),
            )
        },
    )
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
                    EventNotificationButton(
                        uiState = uiState,
                        onNotificationChanged = onNotificationChanged,
                    )
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
            val event = uiState.event
            when {
                uiState.event != null ->
                    EventDetailContent(
                        event = event,
                        onVenueShopClick = onVenueShopClick,
                        onCollaboratorShopClick = onCollaboratorShopClick,
                        onCollaboratorInstagramClick = onCollaboratorInstagramClick,
                        onWaitingLinkClick = onWaitingLinkClick,
                        onSourceLinkClick = onSourceLinkClick,
                    )

                uiState.isEventLoading -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    event: ShopEvent,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTag(stringResource(ShopEventResourceMapper.dateLabel(event)))
            EventTag(stringResource(ShopEventResourceMapper.typeLabel(event.type)))
        }
        AppText(event.title, style = AppTextStyle.H1, color = GrayColor.C500)
        SectionCard(title = stringResource(Res.string.event_venue)) {
            Column(
                modifier =
                    Modifier
                        .padding(vertical = 12.dp, horizontal = 15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                EventVenueLink(
                    title = event.venueShopName,
                    onClick = { onVenueShopClick(event.venueShopId) },
                )
                EventMapActions(
                    event = event,
                    onKakaoClick = { url -> ExternalUriOpener.open(url) },
                    onNaverClick = { url -> ExternalUriOpener.open(url) },
                    onAppleClick = { latitude, longitude ->
                        ExternalUriOpener.openAppleMaps(
                            name = event.venueShopName,
                            address = event.venueAddress,
                            latitude = latitude,
                            longitude = longitude,
                        )
                    },
                )
                event.collaboratorName?.takeIf(String::isNotBlank)?.let { name ->
                    EventSection(
                        stringResource(ShopEventResourceMapper.collaboratorLabel(event)),
                    ) {
                        EventVenueLink(
                            title = name,
                            onClick = {
                                val collaboratorShopId = event.collaboratorShopId

                                if (!collaboratorShopId.isNullOrBlank()) {
                                    onCollaboratorShopClick(collaboratorShopId)
                                    return@EventVenueLink
                                }

                                event.collaboratorInstagramUrl?.let {
                                    onCollaboratorInstagramClick(it)
                                }
                            },
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)

                EventSection(stringResource(Res.string.event_date)) {
                    EventValue(eventDateText(event.startDate, event.endDate))
                }
            }
        }
        event.description.takeIf(String::isNotBlank)?.let { content ->
            SectionCard(title = stringResource(Res.string.event_content)) {
                EventValue(
                    content,
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        event.waitingMethod?.takeIf(String::isNotBlank)?.let { waiting ->
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
private fun EventSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(title, style = AppTextStyle.T2, color = GrayColor.C500)
        content()
    }
}

@Composable
private fun EventValue(
    value: String,
    modifier: Modifier = Modifier,
) = AppText(value, modifier = modifier, style = AppTextStyle.B2, color = GrayColor.C500)

@Composable
private fun EventVenueLink(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().noRippleClickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(
            text = title,
            style = AppTextStyle.T2,
            color = GrayColor.C500,
            modifier = Modifier.weight(1f),
        )
        AppText(
            ">",
            style = AppTextStyle.T2,
            color = GrayColor.C400,
            textAlign = TextAlign.End,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventDetailRoutePreview() {
    val sampleEvent =
        ShopEvent(
            id = "1",
            type = ShopEventType.COLLAB,
            title = "라멘 팝업 이벤트",
            description = "맛있는 라멘 팝업 이벤트입니다. 많은 참여 부탁드립니다.",
            startDate = "2026-07-28",
            endDate = "2026-07-30",
            sourceUrl = "https://www.instagram.com/ramap_official/",
            isToday = true,
            isVenue = true,
            venueShopId = "shop1",
            venueShopName = "이리에 라멘",
            venueAddress = "서울시 마포구",
            collaboratorShopId = "shop2",
            collaboratorName = "콜라보 샵",
            collaboratorInstagramUrl = "https://www.instagram.com/collab_shop/",
            waitingMethod = "현장 대기",
            waitingUrl = "https://catchtable.co.kr/",
        )

    RamapTheme {
        EventDetailScreen(
            uiState =
                EventDetailUiState(
                    event = sampleEvent,
                    isNotificationVisible = true,
                    canChangeNotification = true,
                    isNotificationEnabled = false,
                ),
            onBack = {},
            onVenueShopClick = {},
            onCollaboratorShopClick = {},
            onCollaboratorInstagramClick = {},
            onWaitingLinkClick = {},
            onSourceLinkClick = {},
            onNotificationChanged = {},
        )
    }
}
