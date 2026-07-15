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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.topbar.CommonTopBar
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopEventType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.ui.main.component.SectionCard
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_collaborator_person
import ramap.shared.generated.resources.event_collaborator_shop
import ramap.shared.generated.resources.event_content
import ramap.shared.generated.resources.event_date
import ramap.shared.generated.resources.event_detail_title
import ramap.shared.generated.resources.event_instagram_action
import ramap.shared.generated.resources.event_status_today
import ramap.shared.generated.resources.event_status_upcoming
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_venue
import ramap.shared.generated.resources.event_waiting
import ramap.shared.generated.resources.event_waiting_action
import ramap.shared.generated.resources.ic_arrow3_left
import ramap.shared.generated.resources.navigation_back

@Composable
fun EventDetailRoute(
    event: ShopEvent,
    onBack: () -> Unit,
    onShopClick: (String) -> Unit,
) {
    EventDetailScreen(event, onBack, onShopClick)
}

@Composable
fun EventDetailScreen(
    event: ShopEvent,
    onBack: () -> Unit,
    onShopClick: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val openExternalUri: (String) -> Unit = { uri ->
        uri.takeIf(::isSupportedExternalUri)?.let { runCatching { uriHandler.openUri(it) } }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        topBar = {
            CommonTopBar(
                title = stringResource(Res.string.event_detail_title),
                left = {
                    Image(
                        painter = painterResource(Res.drawable.ic_arrow3_left),
                        contentDescription = stringResource(Res.string.navigation_back),
                        modifier = Modifier.padding(18.dp).size(24.dp).noRippleClickable(onClick = onBack),
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
                    EventLink(event.venueShopName, event.venueAddress) { onShopClick(event.venueShopId) }
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
                                event.collaboratorShopId?.takeIf(String::isNotBlank)?.let(onShopClick)
                                    ?: event.collaboratorInstagramUrl?.let(openExternalUri)
                            }
                        }
                    }
                    EventSection(stringResource(Res.string.event_date)) { EventValue(event.formattedDate) }
                }
            }
            SectionCard(title = stringResource(Res.string.event_content)) {
                EventValue(event.description, Modifier.padding(top = 16.dp))
            }
            event.waitingMethod?.let { waiting ->
                SectionCard(title = stringResource(Res.string.event_waiting)) {
                    EventValue(waiting, Modifier.padding(top = 16.dp))
                    event.waitingUrl?.takeIf(::isSupportedExternalUri)?.let { url ->
                        AppButton(
                            text = stringResource(Res.string.event_waiting_action),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            onClick = { openExternalUri(url) },
                        )
                    }
                }
            }
            if (isSupportedExternalUri(event.sourceUrl)) {
                AppButton(
                    text = stringResource(Res.string.event_instagram_action),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(INSTAGRAM_GRADIENT, RoundedCornerShape(12.dp)),
                    backgroundColor = Color.Transparent,
                    onClick = { openExternalUri(event.sourceUrl) },
                )
            }
        }
    }
}

private fun isSupportedExternalUri(uri: String): Boolean {
    val normalized = uri.trim().lowercase()
    return normalized.startsWith("https://") || normalized.startsWith("http://")
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
        modifier = Modifier.background(ChromaticColor.Yellow400, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
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
