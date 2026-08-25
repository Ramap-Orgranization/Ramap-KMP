package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.detail.contract.EventDetailUiState
import com.peto.ramap.ui.main.event.detail.preview.EventDetailPreviewParameterProvider
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_event_calendar
import ramap.shared.generated.resources.instagram_icon

@Composable
fun EventDetailInfoSection(
    event: ShopEvent,
    hasCollaborators: Boolean,
    onVenueShopClick: (String) -> Unit,
    onCollaboratorShopClick: (String) -> Unit,
    onCollaboratorInstagramClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .background(ChromaticColor.Blue400.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_event_calendar),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ChromaticColor.Blue400,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AppText(
                        text = stringResource(ShopEventResourceMapper.dateTitle(event.type)),
                        style = AppTextStyle.B1,
                        color = ChromaticColor.Blue400,
                    )
                    AppText(
                        text = eventDateText(event.startDate, event.endDate),
                        style = AppTextStyle.B1,
                        color = GrayColor.C500,
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = GrayColor.C050)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VenueShopInfo(
                    shop = event.venueShop,
                    label = stringResource(ShopEventResourceMapper.venueTitle(event.type)),
                    onClick = { onVenueShopClick(event.venueShop.id) },
                )

                if (hasCollaborators) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        event.collaboratorShops.forEach { shop ->
                            VenueShopInfo(
                                shop = shop,
                                label = stringResource(ShopEventResourceMapper.collaboratorLabel(event)),
                                onClick = { onCollaboratorShopClick(shop.id) },
                            )
                        }
                        event.externalParticipants.forEach { participant ->
                            EventVenueLink(
                                title = participant.name,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                onClick = { onCollaboratorInstagramClick(participant.instagramUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VenueShopInfo(
    shop: RamenShop,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = label,
            style = AppTextStyle.T3,
            color = ChromaticColor.Blue400,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .noRippleClickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteShopImage(
                url = shop.instagramProfileImageUrl,
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(12.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AppText(
                        text = shop.name,
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GrayColor.C200,
                    )
                }
                AppText(
                    text = shop.address,
                    style = AppTextStyle.B4,
                    color = GrayColor.C300,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EventVenueLink(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.then(Modifier.noRippleClickable(onClick = onClick)),
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

@Preview(showBackground = true)
@Composable
private fun EventDetailInfoSectionPreview(
    @PreviewParameter(EventDetailPreviewParameterProvider::class)
    uiState: EventDetailUiState,
) {
    RamapTheme {
        uiState.event?.let { event ->
            EventDetailInfoSection(
                event = event,
                hasCollaborators = uiState.hasCollaborators,
                onVenueShopClick = {},
                onCollaboratorShopClick = {},
                onCollaboratorInstagramClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
