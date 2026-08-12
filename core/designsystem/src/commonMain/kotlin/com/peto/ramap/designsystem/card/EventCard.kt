package com.peto.ramap.designsystem.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_status_cancelled
import ramap.shared.generated.resources.event_type_collab
import ramap.shared.generated.resources.event_type_limited_menu
import ramap.shared.generated.resources.event_type_popup
import ramap.shared.generated.resources.event_type_summer_limited
import ramap.shared.generated.resources.ic_close

@Composable
fun EventCard(
    event: ShopEvent,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isCancelled: Boolean = event.isCancelledToday,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier =
            modifier
                .clip(cardShape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, cardShape)
                .clickable(onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteShopImage(
                    url = event.venueProfileImageUrl,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .border(1.dp, GrayColor.C100, CircleShape)
                            .clip(CircleShape),
                )
                AppText(
                    text = event.venueShopName,
                    style = AppTextStyle.B1,
                    color = GrayColor.C400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCancelled) {
                EventTag(stringResource(Res.string.event_status_cancelled))
            }
            EventTag(eventTypeLabel(event.type))
            if (actionLabel != null && onAction != null) {
                IconButton(
                    onClick = onAction,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "${event.title} $actionLabel"
                        },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = null,
                        tint = GrayColor.C400,
                    )
                }
            }
        }
        AppText(
            text = event.title,
            style = AppTextStyle.T2,
            color = GrayColor.C500,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
        AppText(dateText, style = AppTextStyle.B4, color = GrayColor.C400)
    }
}

@Composable
private fun EventTag(text: String) {
    AppText(
        text = text,
        modifier =
            Modifier
                .background(ChromaticColor.Yellow400, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        style = AppTextStyle.C2,
        color = GrayColor.C500,
    )
}

@Composable
private fun eventTypeLabel(type: ShopEventType): String =
    stringResource(
        when (type) {
            ShopEventType.COLLAB -> Res.string.event_type_collab
            ShopEventType.POPUP -> Res.string.event_type_popup
            ShopEventType.LIMITED_MENU -> Res.string.event_type_limited_menu
            ShopEventType.SUMMER_LIMITED -> Res.string.event_type_summer_limited
        },
    )
