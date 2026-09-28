package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.wating.WaitingSystemUiModel
import com.peto.ramap.designsystem.review.ReviewsContent
import com.peto.ramap.designsystem.shop.model.ShopDetailTab
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme

@Composable
fun RamenShopOverview(
    shop: RamenShop,
    likeCount: Long,
    waitingSystem: WaitingSystemUiModel?,
    isBookmarked: Boolean,
    isNotificationEnabled: Boolean,
    showNotificationActions: Boolean,
    isHidden: Boolean,
    isAppleMapsAvailable: Boolean,
    event: ShopEvent?,
    operatingNotice: OperatingNotice?,
    operatingNotices: List<OperatingNotice>,
    menuSections: List<MenuSection>,
    menuUpdatedAt: String?,
    reviews: List<Review>,
    reviewCount: Int,
    menuItemCount: Int,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
    onBookmarkClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareClick: () -> Unit,
    onMapLinkClick: (String) -> Unit,
    onWaitingClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onAppleMapsClick: (RamenShop) -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onOperatingNoticeClick: (OperatingNotice) -> Unit,
    onOpenProfile: (String) -> Unit,
    onWriteReviewClick: () -> Unit,
    menuFooter: @Composable () -> Unit = {},
) {
    var selectedTab by remember(shop.id) { mutableStateOf(ShopDetailTab.MENU) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShopHeaderSection(
            shop = shop,
            likeCount = likeCount,
            isBookmarked = isBookmarked,
            isNotificationEnabled = isNotificationEnabled,
            showNotificationActions = showNotificationActions,
            isHidden = isHidden,
            event = event,
            dragAreaModifier = dragAreaModifier,
            onBookmarkClick = onBookmarkClick,
            onNotificationClick = onNotificationClick,
            onHiddenClick = onHiddenClick,
            onReportClick = onReportClick,
            onShareClick = onShareClick,
            onEventClick = onEventClick,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (shop.businessHoursDetails != null || operatingNotice != null) {
                BusinessHoursCard(
                    shop = shop,
                    operatingNotice = operatingNotice,
                    operatingNotices = operatingNotices,
                    onOperatingNoticeClick = onOperatingNoticeClick,
                )
            }
        }

        ShopExternalLinksRow(
            shop = shop,
            waitingSystem = waitingSystem,
            isAppleMapsAvailable = isAppleMapsAvailable,
            onMapLinkClick = onMapLinkClick,
            onWaitingClick = onWaitingClick,
            onExternalLinkClick = onExternalLinkClick,
            onAppleMapsClick = onAppleMapsClick,
        )

        ShopDetailTabRow(
            menuCount = menuItemCount,
            reviewCount = reviewCount,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        when (selectedTab) {
            ShopDetailTab.MENU -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    ShopMenuContent(
                        sections = menuSections,
                        updatedAt = menuUpdatedAt,
                        onMenuSourceClick = onExternalLinkClick,
                    )
                }
                menuFooter()
            }
            ShopDetailTab.REVIEW -> {
                ReviewsContent(
                    shopName = shop.name,
                    reviews = reviews,
                    onOpenProfile = onOpenProfile,
                    onWriteReviewClick = onWriteReviewClick,
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
            likeCount = 0L,
            waitingSystem = null,
            isBookmarked = false,
            isNotificationEnabled = false,
            showNotificationActions = true,
            isHidden = false,
            isAppleMapsAvailable = true,
            event = null,
            operatingNotice = null,
            operatingNotices = emptyList(),
            menuSections = emptyList(),
            menuUpdatedAt = null,
            reviews = emptyList(),
            reviewCount = 0,
            menuItemCount = 0,
            dragAreaModifier = Modifier,
            onBookmarkClick = {},
            onNotificationClick = {},
            onHiddenClick = {},
            onReportClick = {},
            onShareClick = {},
            onMapLinkClick = {},
            onWaitingClick = {},
            onExternalLinkClick = {},
            onAppleMapsClick = {},
            onEventClick = {},
            onOperatingNoticeClick = {},
            onOpenProfile = {},
            onWriteReviewClick = {},
        )
    }
}
