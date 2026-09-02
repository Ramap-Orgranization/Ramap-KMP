package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.component.MenuCategoryLabels
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.apple_maps_icon
import ramap.shared.generated.resources.ic_kid_star_filled
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
    likeCount: Long = 0L,
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
    onExternalLinkClick: (String) -> Unit,
    isAppleMapsAvailable: Boolean = false,
    onAppleMapsClick: (RamenShop) -> Unit,
    event: ShopEvent? = null,
    onEventClick: (ShopEvent) -> Unit,
    operatingNotice: OperatingNotice? = null,
    onOperatingNoticeClick: (OperatingNotice) -> Unit = {},
    menuSections: List<MenuSection> = emptyList(),
    menuUpdatedAt: String? = null,
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
                NewsBadge(
                    text = ShopOperatingNoticeResourceMapper.notice(notice).format(),
                    modifier =
                        Modifier
                            .padding(top = 5.dp)
                            .padding(horizontal = 24.dp)
                            .noRippleClickable { onOperatingNoticeClick(notice) },
                    textStyle = AppTextStyle.B3,
                    containerColor = SystemColor.Warning,
                    contentColor = CommonColor.White,
                )
            }
            event?.let { shopEvent ->
                ShopEventResourceMapper.notice(shopEvent)?.let { notice ->
                    AppText(
                        text = notice.format(),
                        modifier =
                            Modifier
                                .padding(top = 5.dp)
                                .padding(horizontal = 24.dp)
                                .noRippleClickable { onEventClick(shopEvent) },
                        style = AppTextStyle.B1,
                        color = SystemColor.Warning,
                    )
                }
            }
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                                ).size(45.dp)
                                .clip(CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        AppText(
                            text = shop.name,
                            style = AppTextStyle.H4,
                            color = GrayColor.C500,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.ic_kid_star_filled),
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                colorFilter = ColorFilter.tint(GrayColor.C400),
                            )
                            AppText(
                                text = "$likeCount",
                                style = AppTextStyle.B1,
                                color = GrayColor.C400,
                            )
                        }
                    }

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
                            .padding(horizontal = 20.dp),
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp),
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
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (shop.businessHoursDetails != null) {
                BusinessHoursCard(
                    shop = shop,
                    operatingNotice = operatingNotice,
                )
            }

            ShopMenuContent(
                sections = menuSections,
                updatedAt = menuUpdatedAt,
                onMenuSourceClick = onExternalLinkClick,
            )
        }

        HorizontalDivider(
            thickness = 2.dp,
            color = GrayColor.C100,
            modifier = Modifier.padding(vertical = 5.dp),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (shop.instagramUrl != null || waitingSystem != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shop.instagramUrl?.let { url ->
                        ShopLinkRow(
                            icon = Res.drawable.instagram_icon,
                            label = stringResource(Res.string.shop_detail_link_instagram),
                            onClick = { onExternalLinkClick(url) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    waitingSystem?.let { waiting ->
                        ShopLinkRow(
                            label = stringResource(Res.string.shop_detail_label_waiting),
                            icon = waiting.icon,
                            onClick = { onWaitingClick(waiting.providerUrl) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (shop.kakaoPlaceUrl != null || shop.naverPlaceUrl != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shop.kakaoPlaceUrl?.let { url ->
                        ShopLinkRow(
                            icon = Res.drawable.kakao_map_icon,
                            label = stringResource(Res.string.shop_detail_link_kakao_map),
                            onClick = {
                                onMapLinkClick("kakao")
                                onExternalLinkClick(url)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    shop.naverPlaceUrl?.let { url ->
                        ShopLinkRow(
                            icon = Res.drawable.naver_map_icon,
                            label = stringResource(Res.string.shop_detail_link_naver_map),
                            onClick = {
                                onMapLinkClick("naver")
                                onExternalLinkClick(url)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isAppleMapsAvailable) {
                    ShopLinkRow(
                        icon = Res.drawable.apple_maps_icon,
                        label = stringResource(Res.string.shop_detail_link_apple_maps),
                        onClick = {
                            onMapLinkClick("apple")
                            onAppleMapsClick(shop)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                ShopLinkRow(
                    icon = Res.drawable.ic_report,
                    label = stringResource(Res.string.shop_detail_link_report),
                    onClick = onReportClick,
                    modifier = Modifier.weight(1f),
                )
            }
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
            isAppleMapsAvailable = true,
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
            onExternalLinkClick = {},
            onAppleMapsClick = {},
            event = null,
            onEventClick = {},
            operatingNotice = null,
            onOperatingNoticeClick = {},
        )
    }
}
