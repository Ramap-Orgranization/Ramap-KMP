package com.peto.ramap.designsystem.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.event.ShopEvents
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.stringResource

@Composable
fun EventShopGroupCard(
    eventGroup: ShopEvents,
    onEventClick: (ShopEvent) -> Unit,
    modifier: Modifier = Modifier,
    isCancelled: (ShopEvent) -> Boolean = ShopEvent::isCancelledToday,
    isSoldOut: (ShopEvent) -> Boolean = ShopEvent::isSoldOutToday,
    showHeaderCancelledBadge: Boolean = true,
) {
    val shop = eventGroup.representativeEvent
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, cardShape)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RemoteShopImage(url = shop.venueProfileImageUrl, modifier = Modifier.size(44.dp))
            AppText(
                text = shop.venueShopName,
                modifier = Modifier.weight(1f),
                style = AppTextStyle.B1,
                color = GrayColor.C400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showHeaderCancelledBadge && eventGroup.any(isCancelled)) {
                ShopEventResourceMapper
                    .statusLabel(
                        isCancelled = true,
                        isSoldOut = false,
                    )?.let { statusLabel ->
                        NewsBadge(
                            text = stringResource(statusLabel),
                            containerColor = SystemColor.Warning,
                            contentColor = CommonColor.White,
                        )
                    }
            }
        }
        eventGroup.forEachIndexed { index, event ->
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
            }
            Column(
                modifier = Modifier.fillMaxWidth().noRippleClickable { onEventClick(event) },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppText(
                        text = event.title,
                        modifier = Modifier.weight(1f),
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ShopEventResourceMapper
                        .statusLabel(isCancelled(event), isSoldOut(event))
                        ?.let { statusLabel ->
                            NewsBadge(
                                text = stringResource(statusLabel),
                                containerColor = SystemColor.Warning,
                                contentColor = CommonColor.White,
                            )
                        }
                    NewsBadge(text = stringResource(ShopEventResourceMapper.typeLabel(event.type)))
                }
                AppText(
                    text =
                        eventDateText(
                            event.startDate,
                            if (event.type == ShopEventType.STORE_RENEWAL) event.startDate else event.endDate,
                        ),
                    style = AppTextStyle.B4,
                    color = GrayColor.C400,
                )
            }
        }
    }
}
