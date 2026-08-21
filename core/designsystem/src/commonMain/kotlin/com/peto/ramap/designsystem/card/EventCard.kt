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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close

@Composable
fun EventCard(
    event: ShopEvent,
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isCancelled: Boolean = event.isCancelledToday,
    isSoldOut: Boolean = event.isSoldOutToday,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
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
                    url = event.venueShop.instagramProfileImageUrl,
                    modifier =
                        Modifier
                            .size(44.dp)
                            .border(1.dp, GrayColor.C100, CircleShape)
                            .clip(CircleShape),
                )
                AppText(
                    text = event.venueShop.name,
                    style = AppTextStyle.B1,
                    color = GrayColor.C400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ShopEventResourceMapper.statusLabel(isCancelled, isSoldOut)?.let { statusLabel ->
                NewsBadge(
                    text = stringResource(statusLabel),
                    isStatus = true,
                )
            }
            NewsBadge(stringResource(ShopEventResourceMapper.typeLabel(event.type)))
            if (actionLabel != null && onAction != null) {
                IconButton(onClick = onAction) {
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
