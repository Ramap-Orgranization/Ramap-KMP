package com.peto.ramap.ui.shop.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.ui.shop.overview.component.BusinessHoursCard
import com.peto.ramap.ui.shop.overview.component.ExternalLinksRow
import com.peto.ramap.ui.shop.overview.component.ShopHeaderSection
import com.peto.ramap.ui.shop.overview.model.ExternalLinkActions
import com.peto.ramap.ui.shop.overview.model.HeaderActions
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.overview.model.OverviewActions
import com.peto.ramap.ui.shop.overview.model.OverviewUiState

@Composable
fun OverviewContent(
    uiState: OverviewUiState,
    actions: OverviewActions,
    modifier: Modifier = Modifier,
    dragAreaModifier: Modifier = Modifier,
) {
    val shop = uiState.overview.shop
    val headerState = uiState.headerState
    val headerActions = actions.headerActions
    val externalLinkActions = actions.externalLinkActions

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ShopHeaderSection(
            shop = shop,
            likeCount = uiState.overview.likeCount,
            isBookmarked = headerState.isBookmarked,
            isNotificationEnabled = headerState.isNotificationEnabled,
            showNotificationActions = headerState.showNotificationActions,
            isHidden = headerState.isHidden,
            event = uiState.overview.event,
            dragAreaModifier = dragAreaModifier,
            onDismissRequest = headerActions.onDismissRequest,
            onBookmarkClick = headerActions.onBookmarkClick,
            onNotificationClick = headerActions.onNotificationClick,
            onHiddenClick = headerActions.onHiddenClick,
            onReportClick = headerActions.onReportClick,
            onShareClick = headerActions.onShareClick,
            onEventClick = headerActions.onEventClick,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            if (shop.businessHoursDetails != null || uiState.overview.operatingNotice != null) {
                BusinessHoursCard(
                    shop = shop,
                    operatingNotice = uiState.overview.operatingNotice,
                    operatingNotices = uiState.overview.operatingNotices,
                    onOperatingNoticeClick = headerActions.onOperatingNoticeClick,
                )
            }
        }

        ExternalLinksRow(
            shop = shop,
            waitingSystem = headerState.waitingSystem,
            isAppleMapsAvailable = headerState.isAppleMapsAvailable,
            onMapLinkClick = externalLinkActions.onMapLinkClick,
            onWaitingClick = externalLinkActions.onWaitingClick,
            onExternalLinkClick = externalLinkActions.onExternalLinkClick,
            onAppleMapsClick = externalLinkActions.onAppleMapsClick,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewContentPreview() {
    val shop = RamenShopPreviewParameterProvider().ramenShopPreviewSamples.first()

    OverviewContent(
        uiState =
            OverviewUiState(
                overview =
                    ShopOverView(
                        shop = shop,
                        likeCount = 128L,
                        waitingSystem = null,
                        event = null,
                        operatingNotice = null,
                    ),
                headerState = HeaderUiState(isBookmarked = false),
            ),
        actions =
            OverviewActions(
                headerActions =
                    HeaderActions(
                        onDismissRequest = {},
                        onBookmarkClick = {},
                        onNotificationClick = {},
                        onHiddenClick = {},
                        onReportClick = {},
                        onShareClick = {},
                        onEventClick = {},
                    ),
                externalLinkActions =
                    ExternalLinkActions(
                        onMapLinkClick = {},
                        onWaitingClick = {},
                        onExternalLinkClick = {},
                        onAppleMapsClick = {},
                    ),
            ),
    )
}
