package com.peto.ramap.ui.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.noRippleClickable
import com.peto.ramap.core.extension.stringResource
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.ShopEvent
import com.peto.ramap.domain.model.ShopEventType
import com.peto.ramap.domain.model.ShopWaitingSystem
import com.peto.ramap.domain.model.WaitingProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.main.map.model.WaitingProviderLink
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.catchtable
import ramap.shared.generated.resources.ic_kid_star
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.ic_visibility_off
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.shop_detail_label_address
import ramap.shared.generated.resources.shop_detail_label_business_hours
import ramap.shared.generated.resources.shop_detail_label_phone
import ramap.shared.generated.resources.shop_detail_label_waiting
import ramap.shared.generated.resources.shop_detail_link_instagram
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map
import ramap.shared.generated.resources.shop_detail_link_report
import ramap.shared.generated.resources.shop_detail_waiting_catchtable
import ramap.shared.generated.resources.shop_detail_waiting_syrup_friends
import ramap.shared.generated.resources.shop_detail_waiting_tabling
import ramap.shared.generated.resources.shop_event_notice_collab_participant_today
import ramap.shared.generated.resources.shop_event_notice_collab_participant_upcoming
import ramap.shared.generated.resources.shop_event_notice_collab_today
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming
import ramap.shared.generated.resources.shop_event_notice_collab_upcoming_with_shop
import ramap.shared.generated.resources.shop_event_notice_limited_menu_today
import ramap.shared.generated.resources.shop_event_notice_limited_menu_upcoming
import ramap.shared.generated.resources.shop_event_notice_participant_today
import ramap.shared.generated.resources.shop_event_notice_participant_upcoming
import ramap.shared.generated.resources.shop_event_notice_popup_today
import ramap.shared.generated.resources.shop_event_notice_popup_upcoming
import ramap.shared.generated.resources.syrup_friends
import ramap.shared.generated.resources.tabling

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RamenShopDetailContent(
    shop: RamenShop,
    modifier: Modifier = Modifier,
    waitingSystem: ShopWaitingSystem? = null,
    isBookmarked: Boolean = false,
    isHidden: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    onHiddenClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    event: ShopEvent? = null,
    onEventClick: (ShopEvent) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val waitingProviderLink = waitingSystem?.toWaitingProviderLink()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        event?.let {
            AppText(
                text = it.noticeText(),
                modifier =
                    Modifier
                        .padding(top = 10.dp)
                        .noRippleClickable { onEventClick(it) },
                style = AppTextStyle.B1,
                color = SystemColor.Warning,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AppText(
                    text = shop.name,
                    modifier = Modifier.weight(1f),
                    style = AppTextStyle.H3,
                    color = GrayColor.C500,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isHidden) {
                        ShopActionButton(
                            isActive = isBookmarked,
                            enabled = true,
                            onClick = onBookmarkClick,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_kid_star),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                colorFilter =
                                    ColorFilter.tint(
                                        if (isBookmarked) CommonColor.White else GrayColor.C500,
                                    ),
                            )
                        }
                    }
                    ShopActionButton(
                        isActive = isHidden,
                        enabled = true,
                        onClick = onHiddenClick,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_visibility_off),
                            contentDescription = null,
                            modifier = modifier.size(24.dp),
                            colorFilter =
                                ColorFilter.tint(
                                    if (isHidden) CommonColor.White else GrayColor.C500,
                                ),
                        )
                    }
                }
            }

            if (shop.hasCategory) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    shop.menuCategories.forEach { category ->
                        MenuCategoryChip(label = stringResource(category.stringResource))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShopInfoRow(
                label = stringResource(Res.string.shop_detail_label_address),
                value = shop.address,
            )

            shop.phone?.let { phone ->
                ShopInfoRow(
                    label = stringResource(Res.string.shop_detail_label_phone),
                    value = phone,
                    onClick = { uriHandler.openUri("tel:$phone") },
                )
            }

            shop.businessHours?.let { businessHours ->
                ShopInfoRow(
                    label = stringResource(Res.string.shop_detail_label_business_hours),
                    value = businessHours,
                )
            }

            if (waitingProviderLink != null) {
                ShopIconLinkRow(
                    label = stringResource(Res.string.shop_detail_label_waiting),
                    icon = waitingProviderLink.icon,
                    contentDescription = waitingProviderLink.label,
                    onClick = { uriHandler.openUri(waitingProviderLink.providerUrl) },
                )
            }
        }

        HorizontalDivider(
            thickness = 2.dp,
            color = GrayColor.C100,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            shop.instagramUrl?.let { instagramUrl ->
                ShopLinkRow(
                    icon = Res.drawable.instagram_icon,
                    label = stringResource(Res.string.shop_detail_link_instagram),
                    onClick = { uriHandler.openUri(instagramUrl) },
                )
            }

            shop.kakaoPlaceUrl?.let { kakaoPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.kakao_map_icon,
                    label = stringResource(Res.string.shop_detail_link_kakao_map),
                    onClick = { uriHandler.openUri(kakaoPlaceUrl) },
                )
            }

            shop.naverPlaceUrl?.let { naverPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.naver_map_icon,
                    label = stringResource(Res.string.shop_detail_link_naver_map),
                    onClick = { uriHandler.openUri(naverPlaceUrl) },
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
private fun ShopEvent.noticeText(): String {
    upcomingCollaborationPartnerName?.let { partnerName ->
        return stringResource(Res.string.shop_event_notice_collab_upcoming_with_shop, partnerName)
    }
    if (isVenue) {
        val resource =
            when (type) {
                ShopEventType.COLLAB ->
                    if (isToday) Res.string.shop_event_notice_collab_today else Res.string.shop_event_notice_collab_upcoming
                ShopEventType.POPUP ->
                    if (isToday) Res.string.shop_event_notice_popup_today else Res.string.shop_event_notice_popup_upcoming
                ShopEventType.LIMITED_MENU ->
                    if (isToday) {
                        Res.string.shop_event_notice_limited_menu_today
                    } else {
                        Res.string.shop_event_notice_limited_menu_upcoming
                    }
            }
        return stringResource(resource)
    }
    if (type == ShopEventType.COLLAB) {
        return stringResource(
            if (isToday) {
                Res.string.shop_event_notice_collab_participant_today
            } else {
                Res.string.shop_event_notice_collab_participant_upcoming
            },
            venueShopName,
        )
    }
    return stringResource(
        if (isToday) Res.string.shop_event_notice_participant_today else Res.string.shop_event_notice_participant_upcoming,
        venueShopName,
    )
}

@Composable
private fun ShopActionButton(
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit = {},
    icon: @Composable () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .size(36.dp)
                .alpha(if (enabled) 1f else 0.35f),
        color = if (isActive) GrayColor.C500 else CommonColor.White,
        border =
            if (isActive) {
                null
            } else {
                BorderStroke(width = 1.dp, color = GrayColor.C500)
            },
        shape = CircleShape,
        onClick = {
            if (enabled) {
                onClick()
            } else {
                onDisabledClick()
            }
        },
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun ShopWaitingSystem.toWaitingProviderLink(): WaitingProviderLink? {
    val url = providerUrl ?: return null
    val display =
        when (provider) {
            WaitingProvider.CATCHTABLE ->
                WaitingProviderLink(
                    label = stringResource(Res.string.shop_detail_waiting_catchtable),
                    icon = Res.drawable.catchtable,
                    providerUrl = url,
                )

            WaitingProvider.TABLING ->
                WaitingProviderLink(
                    label = stringResource(Res.string.shop_detail_waiting_tabling),
                    icon = Res.drawable.tabling,
                    providerUrl = url,
                )

            WaitingProvider.SYRUP_FRIENDS ->
                WaitingProviderLink(
                    label = stringResource(Res.string.shop_detail_waiting_syrup_friends),
                    icon = Res.drawable.syrup_friends,
                    providerUrl = url,
                )

            WaitingProvider.UNKNOWN -> null
        }

    return display
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
