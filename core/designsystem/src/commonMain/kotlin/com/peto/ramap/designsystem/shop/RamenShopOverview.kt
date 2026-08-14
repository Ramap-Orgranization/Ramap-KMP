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
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.MenuCategoryLabels
import com.peto.ramap.designsystem.image.RemoteShopImage
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.resource.format
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.event.ShopEventType
import com.peto.ramap.domain.model.shop.BusinessHours
import com.peto.ramap.domain.model.shop.BusinessHoursBreakTime
import com.peto.ramap.domain.model.shop.BusinessHoursDay
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
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
    onBookmarkClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onHiddenClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMapLinkClick: (String) -> Unit = {},
    onPhoneClick: (String) -> Unit = {},
    onWaitingClick: (String) -> Unit = {},
    shouldShowExternalLink: (String) -> Boolean = { true },
    onExternalLinkClick: (String) -> Unit = {},
    isAppleMapsAvailable: Boolean = false,
    onAppleMapsClick: (RamenShop) -> Unit = {},
    event: ShopEvent? = null,
    onEventClick: (ShopEvent) -> Unit = {},
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
private fun RamenShopOverviewPreview() {
    RamapTheme {
        RamenShopOverview(
            shop =
                RamenShop(
                    id = "1",
                    kakaoPlaceId = null,
                    name = "멘야 하나비",
                    address = "서울 강남구 테헤란로 123",
                    location = Location(lat = 37.5, lng = 127.0),
                    kakaoPlaceUrl = "https://place.map.kakao.com/123",
                    naverPlaceUrl = "https://map.naver.com/v5/entry/place/123",
                    phone = "02-123-4567",
                    instagramUrl = "https://instagram.com/menyahana_bi",
                    menuCategories =
                        MenuCategories(
                            listOf(
                                Category.TONKOTSU,
                                Category.TSUKEMEN,
                            ),
                        ),
                    isVisible = true,
                    createdAt = "",
                    updatedAt = "",
                    instagramProfileImageUrl = null,
                    businessHoursDetails =
                        BusinessHours(
                            weekly =
                                mapOf(
                                    "mon" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "tue" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "wed" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "thu" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "fri" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "sat" to BusinessHoursDay(false, "11:00", "21:00", false, null),
                                    "sun" to BusinessHoursDay(true, null, null, false, null),
                                ),
                            breakTimes =
                                mapOf(
                                    "mon" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                    "tue" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                    "wed" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                    "thu" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                    "fri" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                    "sat" to listOf(BusinessHoursBreakTime("14:00", "15:00")),
                                ),
                            lastOrders = emptyMap(),
                            notice = "재료 소진 시 조기 마감될 수 있습니다.",
                        ),
                ),
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
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RamenShopOverviewWithEventPreview() {
    val shop =
        RamenShop(
            id = "1",
            kakaoPlaceId = null,
            name = "멘야 하나비",
            address = "서울 강남구 테헤란로 123",
            location = Location(lat = 37.5, lng = 127.0),
            kakaoPlaceUrl = "https://place.map.kakao.com/123",
            naverPlaceUrl = "https://map.naver.com/v5/entry/place/123",
            phone = "02-123-4567",
            instagramUrl = "https://instagram.com/menyahana_bi",
            menuCategories =
                MenuCategories(
                    listOf(
                        Category.TONKOTSU,
                        Category.TSUKEMEN,
                    ),
                ),
            isVisible = true,
            createdAt = "",
            updatedAt = "",
        )
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
            event =
                ShopEvent(
                    id = "event-1",
                    type = ShopEventType.POPUP,
                    title = "셰프 초청 팝업",
                    description = "특별한 팝업 이벤트입니다.",
                    startDate = "2026-08-12",
                    endDate = "2026-08-16",
                    sourceUrl = "https://www.instagram.com/ramap_official/",
                    isToday = true,
                    isVenue = true,
                    venueShop = shop,
                    collaboratorShops = emptyList(),
                    externalParticipants = emptyList(),
                    waitingMethod = "현장 대기",
                    waitingUrl = "https://catchtable.co.kr/",
                    isCancelledToday = false,
                ),
            onEventClick = {},
        )
    }
}
