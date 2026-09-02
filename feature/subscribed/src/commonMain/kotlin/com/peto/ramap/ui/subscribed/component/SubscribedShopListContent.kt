package com.peto.ramap.ui.subscribed.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.card.EventCard
import com.peto.ramap.designsystem.card.SectionCard
import com.peto.ramap.designsystem.component.RamenShopSummaries
import com.peto.ramap.designsystem.component.ShopListCount
import com.peto.ramap.designsystem.component.ShopListEmptyContent
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.event.ShopEventResourceMapper
import com.peto.ramap.designsystem.text.eventDateText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.preview.ShopEventPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.subscribed.contract.SubscribedShopListUiState
import com.peto.ramap.ui.subscribed.model.SubscribedRemovalTarget
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.notification_removal_confirm_action
import ramap.shared.generated.resources.subscribed_shops_empty_title
import ramap.shared.generated.resources.top_level_tab_event

@Composable
internal fun SubscribedShopListContent(
    uiState: SubscribedShopListUiState,
    onShopOpen: (RamenShop) -> Unit,
    onEventOpen: (ShopEvent) -> Unit,
    onRemovalRequested: (SubscribedRemovalTarget) -> Unit,
) {
    if (uiState.shops.isEmpty() && uiState.subscribedEvents.isEmpty()) {
        ShopListEmptyContent(
            title = stringResource(Res.string.subscribed_shops_empty_title),
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            ShopListCount(count = uiState.shops.size)
            if (uiState.subscribedEvents.isNotEmpty()) {
                SectionCard(
                    title = stringResource(Res.string.top_level_tab_event),
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    uiState.subscribedEvents.forEach { event ->
                        EventCard(
                            event = event,
                            dateText =
                                eventDateText(
                                    event.startDate,
                                    ShopEventResourceMapper.displayEndDate(event),
                                ),
                            onClick = { onEventOpen(event) },
                            actionLabel = stringResource(Res.string.notification_removal_confirm_action),
                            onAction = {
                                onRemovalRequested(SubscribedRemovalTarget.EventOverride(event.id))
                            },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }

            if (uiState.shops.isNotEmpty()) {
                RamenShopSummaries(
                    shops = uiState.shops,
                    onShopClick = onShopOpen,
                    categoryLabel = { category ->
                        stringResource(CategoryResourceMapper.label(category))
                    },
                    itemActionLabel = { _ -> stringResource(Res.string.notification_removal_confirm_action) },
                    onItemAction = {
                        onRemovalRequested(SubscribedRemovalTarget.Shop(it.id))
                    },
                    modifier = Modifier.padding(horizontal = 15.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscribedShopListContentPreview() {
    val shopProvider = RamenShopPreviewParameterProvider()
    val eventProvider = ShopEventPreviewParameterProvider()

    RamapTheme {
        SubscribedShopListContent(
            uiState =
                SubscribedShopListUiState(
                    shops = RamenShops(shopProvider.ramenShopPreviewSamples),
                    subscribedEvents = eventProvider.values.toList(),
                    haveShopsLoaded = true,
                    haveEventsLoaded = true,
                ),
            onShopOpen = {},
            onEventOpen = {},
            onRemovalRequested = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscribedShopListContentEmptyPreview() {
    RamapTheme {
        SubscribedShopListContent(
            uiState =
                SubscribedShopListUiState(
                    haveShopsLoaded = true,
                    haveEventsLoaded = true,
                ),
            onShopOpen = {},
            onEventOpen = {},
            onRemovalRequested = {},
        )
    }
}
