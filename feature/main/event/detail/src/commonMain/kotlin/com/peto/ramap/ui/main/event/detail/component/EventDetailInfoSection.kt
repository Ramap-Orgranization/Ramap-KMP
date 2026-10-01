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
import com.peto.ramap.domain.model.event.EventVenue
import com.peto.ramap.domain.model.event.ShopEvent
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
import ramap.shared.generated.resources.ic_operating_notice_fab
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.shop_detail_link_instagram
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map

@Composable
fun EventDetailInfoSection(
    event: ShopEvent,
    hasCollaborators: Boolean,
    onVenueShopClick: (String) -> Unit,
    onVenueInstagramClick: (String) -> Unit,
    onVenueNaverMapClick: (String) -> Unit,
    onVenueKakaoMapClick: (String) -> Unit,
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
                        painter = painterResource(Res.drawable.ic_operating_notice_fab),
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
                        text = eventDateText(event.startDate, ShopEventResourceMapper.displayEndDate(event)),
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
                when (val venue = event.venue) {
                    is EventVenue.Registered ->
                        VenueShopInfo(
                            name = venue.shop.name,
                            imageUrl = venue.shop.instagramProfileImageUrl,
                            label = stringResource(ShopEventResourceMapper.venueTitle(event.type)),
                            address = venue.shop.address,
                            onClick = { onVenueShopClick(venue.shop.id) },
                        )
                    is EventVenue.External ->
                        VenueShopInfo(
                            name = venue.name,
                            imageUrl = venue.imageUrl,
                            label = stringResource(ShopEventResourceMapper.venueTitle(event.type)),
                            address = venue.address,
                            onInstagramClick = venue.instagramUrl?.let { { onVenueInstagramClick(it) } },
                            onNaverMapClick = venue.naverMapUrl?.let { { onVenueNaverMapClick(it) } },
                            onKakaoMapClick = venue.kakaoMapUrl?.let { { onVenueKakaoMapClick(it) } },
                        )
                }

                if (hasCollaborators) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        event.collaboratorShops.forEach { shop ->
                            VenueShopInfo(
                                name = shop.name,
                                imageUrl = shop.instagramProfileImageUrl,
                                label = stringResource(ShopEventResourceMapper.collaboratorLabel(event)),
                                address = shop.address,
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
    name: String,
    imageUrl: String?,
    label: String,
    address: String? = null,
    onClick: (() -> Unit)? = null,
    onInstagramClick: (() -> Unit)? = null,
    onNaverMapClick: (() -> Unit)? = null,
    onKakaoMapClick: (() -> Unit)? = null,
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
                    .then(if (onClick == null) Modifier else Modifier.noRippleClickable(onClick = onClick)),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteShopImage(
                url = imageUrl,
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
                        text = name,
                        style = AppTextStyle.T2,
                        color = GrayColor.C500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (onClick != null) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GrayColor.C200,
                        )
                    }
                }
                if (address != null) {
                    AppText(
                        text = address,
                        style = AppTextStyle.B4,
                        color = GrayColor.C300,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VenueLinkIcons(onInstagramClick, onNaverMapClick, onKakaoMapClick)
            }
        }
    }
}

@Composable
private fun EventVenueLink(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.then(if (onClick == null) Modifier else Modifier.noRippleClickable(onClick = onClick)),
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

@Composable
private fun VenueLinkIcons(
    onInstagramClick: (() -> Unit)?,
    onNaverMapClick: (() -> Unit)?,
    onKakaoMapClick: (() -> Unit)?,
) {
    if (onInstagramClick == null && onNaverMapClick == null && onKakaoMapClick == null) return

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        onInstagramClick?.let {
            VenueIcon(
                icon = Res.drawable.instagram_icon,
                contentDescription = stringResource(Res.string.shop_detail_link_instagram),
                onClick = it,
            )
        }
        onNaverMapClick?.let {
            VenueIcon(
                icon = Res.drawable.naver_map_icon,
                contentDescription = stringResource(Res.string.shop_detail_link_naver_map),
                onClick = it,
            )
        }
        onKakaoMapClick?.let {
            VenueIcon(
                icon = Res.drawable.kakao_map_icon,
                contentDescription = stringResource(Res.string.shop_detail_link_kakao_map),
                onClick = it,
            )
        }
    }
}

@Composable
private fun VenueIcon(
    icon: org.jetbrains.compose.resources.DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(40.dp).noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(icon), contentDescription, Modifier.size(24.dp))
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
                onVenueInstagramClick = {},
                onVenueNaverMapClick = {},
                onVenueKakaoMapClick = {},
                onCollaboratorShopClick = {},
                onCollaboratorInstagramClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
