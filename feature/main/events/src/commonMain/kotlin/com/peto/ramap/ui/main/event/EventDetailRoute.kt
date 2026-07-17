package com.peto.ramap.ui.main.event

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.LaduckLoadingContent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.component.eventDateText
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnEntered
import com.peto.ramap.ui.main.event.contract.EventDetailIntent.OnNotificationChanged
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.EventUnavailable
import com.peto.ramap.ui.main.event.contract.EventDetailSideEffect.ShowEventToast
import com.peto.ramap.ui.main.event.contract.EventDetailUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_collaborator_person
import ramap.shared.generated.resources.event_collaborator_shop
import ramap.shared.generated.resources.event_content
import ramap.shared.generated.resources.event_date
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.event_instagram_action
import ramap.shared.generated.resources.event_notification_disable
import ramap.shared.generated.resources.event_notification_enable
import ramap.shared.generated.resources.event_status_today
import ramap.shared.generated.resources.event_status_upcoming
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_venue
import ramap.shared.generated.resources.event_waiting
import ramap.shared.generated.resources.event_waiting_action
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_notification_filled
import ramap.shared.generated.resources.navigation_back

@Composable
fun EventDetailRoute(
    eventId: String,
    initialEvent: ShopEvent?,
    onBack: () -> Unit,
    onUnavailable: () -> Unit,
    onShopClick: (String) -> Unit,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    viewModel: EventDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(eventId, initialEvent) {
        viewModel.dispatch(OnEntered(eventId, initialEvent))
    }

    ObserveAsEvents(viewModel.sideEffect) { sideEffect ->
        when (sideEffect) {
            EventUnavailable -> onUnavailable()
            is ShowEventToast ->
                toastManager.show(
                    sideEffect.data.copy(
                        action = sideEffect.data.action?.copy(onClick = appSettingsOpener::open),
                    ),
                )
        }
    }
    uiState.event?.let {
        EventDetailScreen(
            event = it,
            onBack = onBack,
            onShopClick = onShopClick,
            uiState = uiState,
            onNotificationChanged = { enabled -> viewModel.dispatch(OnNotificationChanged(enabled)) },
        )
    } ?: LaduckLoadingContent()
}

@Composable
fun EventDetailScreen(
    event: ShopEvent,
    onBack: () -> Unit,
    onShopClick: (String) -> Unit,
    uiState: EventDetailUiState,
    onNotificationChanged: (Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventTag(stringResource(if (event.isToday) Res.string.event_status_today else Res.string.event_status_upcoming))
                EventTag(eventTypeLabel(event.type))
            }
            AppText(event.title, style = AppTextStyle.H1, color = GrayColor.C500)
            SectionCard(title = stringResource(Res.string.event_venue)) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    EventLink(
                        event.venueShopName,
                        event.venueAddress,
                    ) { onShopClick(event.venueShopId) }
                    event.collaboratorName?.takeIf(String::isNotBlank)?.let { name ->
                        EventSection(
                            stringResource(
                                if (event.collaboratorShopId.isNullOrBlank()) {
                                    Res.string.event_collaborator_person
                                } else {
                                    Res.string.event_collaborator_shop
                                },
                            ),
                        ) {
                            EventLink(name) {
                                event.collaboratorShopId
                                    ?.takeIf(String::isNotBlank)
                                    ?.let(onShopClick)
                                    ?: event.collaboratorInstagramUrl?.let(ExternalUriOpener::open)
                            }
                        }
                    }
                    EventSection(stringResource(Res.string.event_date)) {
                        EventValue(eventDateText(event.startDate, event.endDate))
                    }
                }
            }
            SectionCard(title = stringResource(Res.string.event_content)) {
                EventValue(event.description, Modifier.padding(top = 16.dp))
            }
            event.waitingMethod?.let { waiting ->
                SectionCard(title = stringResource(Res.string.event_waiting)) {
                    EventValue(waiting, Modifier.padding(top = 16.dp))
                    event.waitingUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { url ->
                        AppButton(
                            text = stringResource(Res.string.event_waiting_action),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            onClick = { ExternalUriOpener.open(url) },
                        )
                    }
                }
            }
            if (ExternalUriOpener.isSupportedWebUri(event.sourceUrl)) {
                AppButton(
                    text = stringResource(Res.string.event_instagram_action),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(INSTAGRAM_GRADIENT, RoundedCornerShape(12.dp)),
                    backgroundColor = Color.Transparent,
                    onClick = { ExternalUriOpener.open(event.sourceUrl) },
                )
            }
        }
    }
}

@Composable
private fun EventNotificationButton(
    uiState: EventDetailUiState,
    onNotificationChanged: (Boolean) -> Unit,
) {
    if (!uiState.isNotificationVisible) return
    IconButton(
        enabled = uiState.canChangeNotification && !uiState.isNotificationLoading,
        onClick = { onNotificationChanged(!uiState.isNotificationEnabled) },
    ) {
        Icon(
            painter =
                painterResource(
                    if (uiState.isNotificationEnabled) {
                        Res.drawable.ic_notification_filled
                    } else {
                        Res.drawable.ic_notification
                    },
                ),
            contentDescription =
                stringResource(
                    if (uiState.isNotificationEnabled) {
                        Res.string.event_notification_disable
                    } else {
                        Res.string.event_notification_enable
                    },
                ),
            tint =
                if (uiState.isNotificationEnabled && !uiState.isNotificationLoading) {
                    InstagramColor.Pink
                } else {
                    GrayColor.C300
                },
        )
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
private fun EventTag(text: String) {
    AppText(
        text,
        modifier =
            Modifier
                .background(ChromaticColor.Yellow400, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        style = AppTextStyle.B3,
        color = GrayColor.C500,
    )
}

@Composable
private fun EventSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(title, style = AppTextStyle.T3, color = GrayColor.C300)
        content()
    }
}

@Composable
private fun EventValue(
    value: String,
    modifier: Modifier = Modifier,
) = AppText(value, modifier = modifier, style = AppTextStyle.B2, color = GrayColor.C500)

@Composable
private fun EventLink(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().noRippleClickable(onClick = onClick)) {
        AppText("$title >", style = AppTextStyle.T2, color = GrayColor.C500)
        subtitle?.let { EventValue(it) }
    }
}

@Composable
private fun eventTypeLabel(type: ShopEventType): String =
    stringResource(
        when (type) {
            ShopEventType.COLLAB -> Res.string.event_type_collab
            ShopEventType.POPUP -> Res.string.event_type_popup
            ShopEventType.LIMITED_MENU -> Res.string.event_type_limited_menu
        },
    )
