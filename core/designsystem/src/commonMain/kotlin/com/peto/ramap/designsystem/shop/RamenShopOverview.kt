package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.MenuCategoryLabels
import com.peto.ramap.designsystem.badge.EventBadge
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.preview.ShopEventPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_maps_icon
import ramap.shared.generated.resources.ic_report
import ramap.shared.generated.resources.instagram_icon
import ramap.shared.generated.resources.kakao_map_icon
import ramap.shared.generated.resources.naver_map_icon
import ramap.shared.generated.resources.shop_detail_copy_address
import ramap.shared.generated.resources.shop_detail_label_address
import ramap.shared.generated.resources.shop_detail_label_phone
import ramap.shared.generated.resources.shop_detail_label_waiting
import ramap.shared.generated.resources.shop_detail_link_apple_maps
import ramap.shared.generated.resources.shop_detail_link_instagram
import ramap.shared.generated.resources.shop_detail_link_kakao_map
import ramap.shared.generated.resources.shop_detail_link_naver_map
import ramap.shared.generated.resources.shop_detail_link_report

@Composable
fun RamenShopOverview(
    shop: RamenShop,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    waitingSystem: WaitingSystemUiModel? = null,
    isBookmarked: Boolean = false,
    isNotificationEnabled: Boolean = false,
    showNotificationActions: Boolean = true,
    isHidden: Boolean = false,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    onMapLinkClick: (String) -> Unit,
    onPhoneClick: (String) -> Unit,
    onWaitingClick: (String) -> Unit,
    shouldShowExternalLink: (String) -> Boolean,
    onExternalLinkClick: (String) -> Unit,
    isAppleMapsAvailable: Boolean = false,
    onAppleMapsClick: (RamenShop) -> Unit,
    event: ShopEvent? = null,
    onEventClick: (ShopEvent) -> Unit,
    operatingNotice: OperatingNotice? = null,
    onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = dragAreaModifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            operatingNotice?.let { notice ->
                EventBadge(
                    text = ShopOperatingNoticeResourceMapper.notice(notice).format(),
                    modifier =
                        Modifier
                            .padding(top = 5.dp)
                            .padding(horizontal = 24.dp)
                            .noRippleClickable { onOperatingNoticeClick(notice) },
                    textStyle = AppTextStyle.B1,
                )
            }
            event?.let { shopEvent ->
                ShopEventResourceMapper.notice(shopEvent)?.let { notice ->
                    EventBadge(
                        text = notice.format(),
                        modifier =
                            Modifier
                                .padding(top = 5.dp)
                                .padding(horizontal = 24.dp)
                                .noRippleClickable { onEventClick(shopEvent) },
                        textStyle = AppTextStyle.B1,
                    )
                }
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
                                .align(Alignment.CenterVertically)
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
                        showNotificationActions = showNotificationActions,
                        isHidden = isHidden,
                        onBookmarkClick = onBookmarkClick,
                        onNotificationClick = onNotificationClick,
                        onHiddenClick = onHiddenClick,
                        onShareClick = onShareClick,
                    )
                }

                MenuCategoryLabels(
                    menuCategories = shop.menuCategories,
                    categoryLabel = { category ->
                        stringResource(
                            CategoryResourceMapper.label(
                                category,
                            ),
                        )
                    },
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
                    onClick = { clipboardManager.setText(AnnotatedString(shop.address)) },
                    onClickLabel = stringResource(Res.string.shop_detail_copy_address),
                )

                shop.phone?.takeIf(String::isNotBlank)?.let { phone ->
                    ShopInfoRow(
                        label = stringResource(Res.string.shop_detail_label_phone),
                        value = phone,
                        onClick = { onPhoneClick(phone) },
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            shop.businessHoursDetails?.let { businessHours ->
                BusinessHoursCard(
                    businessHours = businessHours,
                )
            }

            waitingSystem?.let {
                ShopIconLinkRow(
                    label = stringResource(Res.string.shop_detail_label_waiting),
                    icon = waitingSystem.icon,
                    contentDescription = stringResource(waitingSystem.label),
                    onClick = { onWaitingClick(waitingSystem.providerUrl) },
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
            shop.instagramUrl?.takeIf(shouldShowExternalLink)?.let { instagramUrl ->
                ShopLinkRow(
                    icon = Res.drawable.instagram_icon,
                    label = stringResource(Res.string.shop_detail_link_instagram),
                    onClick = { onExternalLinkClick(instagramUrl) },
                )
            }

            shop.kakaoPlaceUrl?.takeIf(shouldShowExternalLink)?.let { kakaoPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.kakao_map_icon,
                    label = stringResource(Res.string.shop_detail_link_kakao_map),
                    onClick = {
                        onMapLinkClick("kakao")
                        onExternalLinkClick(kakaoPlaceUrl)
                    },
                )
            }

            shop.naverPlaceUrl?.takeIf(shouldShowExternalLink)?.let { naverPlaceUrl ->
                ShopLinkRow(
                    icon = Res.drawable.naver_map_icon,
                    label = stringResource(Res.string.shop_detail_link_naver_map),
                    onClick = {
                        onMapLinkClick("naver")
                        onExternalLinkClick(naverPlaceUrl)
                    },
                )
            }

            if (isAppleMapsAvailable) {
                ShopLinkRow(
                    icon = Res.drawable.apple_maps_icon,
                    label = stringResource(Res.string.shop_detail_link_apple_maps),
                    onClick = {
                        onMapLinkClick("apple")
                        onAppleMapsClick(shop)
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

@Preview(showBackground = true)
@Composable
private fun RamenShopOverviewPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
) {
    RamapTheme {
        RamenShopOverview(
            shop = shop,
            dragAreaModifier = Modifier,
            waitingSystem = null,
            isBookmarked = false,
            isNotificationEnabled = false,
            isHidden = false,
            onBookmarkClick = {},
            onNotificationClick = {},
            onHiddenClick = {},
            onReportClick = {},
            onShareClick = {},
            onMapLinkClick = {},
            onPhoneClick = {},
            onWaitingClick = {},
            shouldShowExternalLink = { true },
            onExternalLinkClick = {},
            isAppleMapsAvailable = false,
            onAppleMapsClick = {},
            event = null,
            onEventClick = {},
            operatingNotice = null,
            onOperatingNoticeClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopOverviewWithEventPreview(
    @PreviewParameter(RamenShopPreviewParameterProvider::class) shop: RamenShop,
    @PreviewParameter(ShopEventPreviewParameterProvider::class) event: ShopEvent,
) {
    RamapTheme {
        RamenShopOverview(
            shop = shop,
            dragAreaModifier = Modifier,
            waitingSystem = null,
            isBookmarked = false,
            isNotificationEnabled = false,
            isHidden = false,
            onBookmarkClick = {},
            onNotificationClick = {},
            onHiddenClick = {},
            onReportClick = {},
            onShareClick = {},
            onMapLinkClick = {},
            onPhoneClick = {},
            onWaitingClick = {},
            shouldShowExternalLink = { true },
            onExternalLinkClick = {},
            isAppleMapsAvailable = false,
            onAppleMapsClick = {},
            event = event,
            onEventClick = {},
            operatingNotice = null,
            onOperatingNoticeClick = {},
        )
    }
}
