package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.MenuCategoryLabels
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.ui.resource.format
import com.peto.ramap.ui.resource.businesshours.BusinessHoursResourceLine
import com.peto.ramap.ui.resource.businesshours.BusinessHoursResourceMapper
import com.peto.ramap.ui.resource.category.CategoryResourceMapper
import com.peto.ramap.ui.resource.event.ShopEventResourceMapper
import com.peto.ramap.ui.resource.wating.WaitingSystemUiModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_maps_icon
import ramap.shared.generated.resources.bookmarked_shops_toggle
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.event_notification_action
import ramap.shared.generated.resources.hide_shop_action
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_kid_star_filled
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_notification
import ramap.shared.generated.resources.ic_notification_filled
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.ic_share
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.share_shop_action
import ramap.shared.generated.resources.shop_detail_business_hours_break_time_format
import ramap.shared.generated.resources.shop_detail_business_hours_closed
import ramap.shared.generated.resources.shop_detail_business_hours_closed_label_format
import ramap.shared.generated.resources.shop_detail_business_hours_weekday_range_format
import ramap.shared.generated.resources.shop_detail_business_hours_weekly_title
import ramap.shared.generated.resources.shop_detail_label_address
import ramap.shared.generated.resources.shop_detail_label_business_hours
import ramap.shared.generated.resources.shop_detail_label_phone
import ramap.shared.generated.resources.shop_detail_label_waiting
import ramap.shared.generated.resources.shop_detail_link_apple_maps
import ramap.shared.generated.resources.shop_detail_link_instagram
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_detail_more_actions
import ramap.shared.generated.resources.shop_detail_waiting_catchtable

@Composable
internal fun RamenShopOverview(
    shop: RamenShop,
    modifier: Modifier = Modifier,
    waitingSystem: WaitingSystemUiModel? = null,
    isBookmarked: Boolean = false,
    isNotificationEnabled: Boolean = false,
    isHidden: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onHiddenClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMapLinkClick: (String) -> Unit = {},
    event: ShopEvent? = null,
    onEventClick: (ShopEvent) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        event?.let {
            AppText(
                text = ShopEventResourceMapper.notice(it).format(),
                modifier =
                    Modifier
                        .padding(top = 5.dp)
                        .padding(horizontal = 24.dp)
                        .noRippleClickable { onEventClick(it) },
                style = AppTextStyle.B1,
                color = SystemColor.Warning,
            )
        }
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                RemoteShopImage(
                    url = shop.instagramProfileImageUrl,
                    modifier =
                        Modifier
                            .border(
                                width = 1.dp,
                                color = GrayColor.C100,
                                shape = RoundedCornerShape(999.dp),
                            ).size(40.dp)
                            .clip(CircleShape),
                )
                AppText(
                    text = shop.name,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(top = 5.dp),
                    style = AppTextStyle.H3,
                    color = GrayColor.C500,
                )
                ShopOverflowMenu(
                    shopId = shop.id,
                    isBookmarked = isBookmarked,
                    isNotificationEnabled = isNotificationEnabled,
                    isHidden = isHidden,
                    onBookmarkClick = onBookmarkClick,
                    onNotificationClick = onNotificationClick,
                    onHiddenClick = onHiddenClick,
                    onShareClick = onShareClick,
                )
            }

            MenuCategoryLabels(
                menuCategories = shop.menuCategories,
                categoryLabel = { category -> stringResource(CategoryResourceMapper.label(category)) },
                style = AppTextStyle.B1,
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .padding(horizontal = 24.dp),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            ShopInfoRow(
                label = stringResource(Res.string.shop_detail_label_address),
                value = shop.address,
            )

            shop.phone?.takeIf(String::isNotBlank)?.let { phone ->
                ShopInfoRow(
                    label = stringResource(Res.string.shop_detail_label_phone),
                    value = phone,
                    onClick = { ExternalUriOpener.open("tel:$phone") },
                )
            }

            shop.businessHoursDetails?.let { businessHours ->
                BusinessHoursCard(
                    businessHours = businessHours,
                    fallbackText = shop.businessHours,
                )
            } ?: shop.businessHours?.let { businessHours ->
                ShopInfoRow(
                    label = stringResource(Res.string.shop_detail_label_business_hours),
                    value = businessHours,
                )
            }

            waitingSystem?.let {
                ShopIconLinkRow(
                    label = stringResource(Res.string.shop_detail_label_waiting),
                    icon = waitingSystem.icon,
                    contentDescription = stringResource(waitingSystem.label),
                    onClick = { ExternalUriOpener.open(waitingSystem.providerUrl) },
                )
            }
        }

        HorizontalDivider(
            thickness = 2.dp,
            color = GrayColor.C100,
            modifier = Modifier.padding(vertical = 5.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            shop.instagramUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { instagramUrl ->
                ShopLinkRow(
                    icon = Res.drawable.instagram_icon,
                    label = stringResource(Res.string.shop_detail_link_instagram),
                    onClick = { ExternalUriOpener.open(instagramUrl) },
                )
            }

            shop.kakaoPlaceUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { kakaoPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.kakao_map_icon,
                    label = stringResource(Res.string.shop_detail_link_kakao_map),
                    onClick = {
                        onMapLinkClick("kakao")
                        ExternalUriOpener.open(kakaoPlaceUrl)
                    },
                )
            }

            shop.naverPlaceUrl?.takeIf(ExternalUriOpener::isSupportedWebUri)?.let { naverPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.naver_map_icon,
                    label = stringResource(Res.string.shop_detail_link_naver_map),
                    onClick = {
                        onMapLinkClick("naver")
                        ExternalUriOpener.open(naverPlaceUrl)
                    },
                )
            }

            if (ExternalUriOpener.isAppleMapsAvailable) {
                ShopLinkRow(
                    icon = Res.drawable.apple_maps_icon,
                    label = stringResource(Res.string.shop_detail_link_apple_maps),
                    onClick = {
                        onMapLinkClick("apple")
                        ExternalUriOpener.openAppleMaps(
                            name = shop.name,
                            address = shop.address,
                            latitude = shop.location.lat,
                            longitude = shop.location.lng,
                        )
                    },
                )
            }

            ShopLinkRow(
                icon = Res.drawable.ic_report,
                label = stringResource(Res.string.shop_detail_link_report),
                onClick = onReportClick,
            )
        }
    }
}

@Composable
private fun ShopOverflowMenu(
    shopId: String,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    isHidden: Boolean,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    var isExpanded by remember(shopId) { mutableStateOf(false) }
    val moreActionsDescription = stringResource(Res.string.shop_detail_more_actions)

    Box {
        Image(
            painter = painterResource(Res.drawable.ic_more_vert),
            contentDescription = moreActionsDescription,
            modifier =
                Modifier
                    .size(40.dp)
                    .noRippleClickable { isExpanded = true }
                    .padding(8.dp),
            colorFilter = ColorFilter.tint(GrayColor.C500),
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.widthIn(min = 150.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = CommonColor.White,
        ) {
            ShopOverflowMenuItem(
                text = stringResource(Res.string.share_shop_action),
                icon = Res.drawable.ic_share,
                isActive = null,
                onClick = {
                    isExpanded = false
                    onShareClick()
                },
            )
            if (!isHidden) {
                ShopOverflowMenuItem(
                    text = stringResource(Res.string.bookmarked_shops_toggle),
                    icon = if (isBookmarked) Res.drawable.ic_kid_star_filled else Res.drawable.ic_kid_star,
                    isActive = isBookmarked,
                    onClick = {
                        onBookmarkClick()
                    },
                )
                ShopOverflowMenuItem(
                    text = stringResource(Res.string.event_notification_action),
                    icon =
                        if (isNotificationEnabled) {
                            Res.drawable.ic_notification_filled
                        } else {
                            Res.drawable.ic_notification
                        },
                    isActive = isNotificationEnabled,
                    onClick = {
                        onNotificationClick()
                    },
                )
            }
            ShopOverflowMenuItem(
                text = stringResource(Res.string.hide_shop_action),
                icon = Res.drawable.ic_visibility_off,
                isActive = isHidden,
                onClick = {
                    isExpanded = false
                    onHiddenClick()
                },
            )
        }
    }
}

@Composable
private fun ShopOverflowMenuItem(
    text: String,
    icon: DrawableResource,
    isActive: Boolean?,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            AppText(
                text = text,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
        },
        leadingIcon = {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter =
                    ColorFilter.tint(
                        when {
                            isActive == true -> InstagramColor.Pink
                            else -> GrayColor.C300
                        },
                    ),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun ShopInfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = GrayColor.C300,
        )
        AppText(
            text = value,
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (onClick == null) {
                            Modifier
                        } else {
                            Modifier.noRippleClickable(onClick = onClick)
                        },
                    ),
            style = AppTextStyle.B2,
            color = GrayColor.C500,
            textDecoration = if (onClick == null) null else TextDecoration.Underline,
        )
    }
}

@Composable
private fun BusinessHoursCard(
    businessHours: BusinessHours,
    fallbackText: String?,
) {
    val lines = BusinessHoursResourceMapper.all(businessHours)
    if (lines.isEmpty()) {
        fallbackText?.let {
            ShopInfoRow(
                label = stringResource(Res.string.shop_detail_label_business_hours),
                value = it,
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = stringResource(Res.string.shop_detail_business_hours_weekly_title),
            style = AppTextStyle.B1,
            color = GrayColor.C300,
        )
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            lines.forEachIndexed { index, line ->
                BusinessHoursCardRow(
                    line = line,
                    isLast = index == lines.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun BusinessHoursCardRow(
    line: BusinessHoursResourceLine,
    isLast: Boolean,
) {
    val mainValue = line.values.firstOrNull() ?: return
    val isClosed =
        mainValue.resource == Res.string.shop_detail_business_hours_closed ||
            mainValue.resource == Res.string.shop_detail_business_hours_closed_label_format

    Column {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = line.dayLabelText(),
                modifier = Modifier.weight(1f),
                style = AppTextStyle.B1,
                color = if (isClosed) SystemColor.Warning else GrayColor.C500,
            )
            Column(horizontalAlignment = Alignment.End) {
                AppText(
                    text = mainValue.format(),
                    style = AppTextStyle.B3,
                    color = if (isClosed) SystemColor.Warning else GrayColor.C500,
                )
                line.values.drop(1).forEach { value ->
                    if (value.resource == Res.string.shop_detail_business_hours_break_time_format) {
                        AppText(
                            text = value.format(),
                            modifier =
                                Modifier
                                    .padding(top = 4.dp)
                                    .background(
                                        color = ChromaticColor.Orange400.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                    ).padding(horizontal = 5.dp, vertical = 2.dp),
                            style = AppTextStyle.C2,
                            color = InstagramColor.Orange,
                        )
                    } else {
                        AppText(
                            text = value.format(),
                            modifier = Modifier.padding(top = 4.dp),
                            style = AppTextStyle.C2,
                            color = GrayColor.C300,
                        )
                    }
                }
            }
        }
        if (!isLast) {
            HorizontalDivider(thickness = 1.dp, color = GrayColor.C100)
        }
    }
}

@Composable
private fun BusinessHoursResourceLine.dayLabelText(): String =
    endDayLabel?.let { endDayLabel ->
        stringResource(
            Res.string.shop_detail_business_hours_weekday_range_format,
            stringResource(dayLabel),
            stringResource(endDayLabel),
        )
    } ?: stringResource(dayLabel)

@Composable
private fun ShopLinkRow(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.noRippleClickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
    }
}

@Composable
private fun ShopIconLinkRow(
    label: String,
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(
            text = label,
            style = AppTextStyle.B1,
            color = GrayColor.C300,
        )
        Image(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .size(28.dp)
                    .noRippleClickable(onClick = onClick),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RamenShopOverviewPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        RamenShopOverview(
            shop = shop,
            waitingSystem =
                WaitingSystemUiModel(
                    label = Res.string.shop_detail_waiting_catchtable,
                    icon = Res.drawable.catchtable,
                    providerUrl = "https://app.catchtable.co.kr",
                ),
            event =
                ShopEvent(
                    id = "1",
                    type = ShopEventType.LIMITED_MENU,
                    title = "한정 메뉴 이벤트",
                    description = "오늘만 판매하는 특별한 라멘!",
                    startDate = "2024-01-01",
                    endDate = "2024-01-02",
                    sourceUrl = "https://instagram.com/p/xxx",
                    isToday = true,
                    isVenue = true,
                    venueShopId = shop.id,
                    venueShopName = shop.name,
                    venueAddress = shop.address,
                    collaboratorShopId = null,
                    collaboratorName = null,
                    collaboratorInstagramUrl = null,
                    waitingMethod = null,
                    waitingUrl = null,
                ),
        )
    }
}
