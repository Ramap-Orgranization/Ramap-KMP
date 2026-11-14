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
import com.peto.ramap.designsystem.component.RamenShopSummaries
import com.peto.ramap.designsystem.component.ShopListCount
import com.peto.ramap.designsystem.component.ShopsEmptyContent
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.preview.RamenShopPreviewParameterProvider
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_notification_settings_title
import ramap.shared.generated.resources.subscribed_shops_empty_title

@Composable
internal fun SubscribedShopListContent(
    shops: RamenShops,
    onShopOpen: (RamenShop) -> Unit,
    onShopNotificationSettingsOpen: (RamenShop) -> Unit,
) {
    if (shops.isEmpty()) {
        ShopsEmptyContent(
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
            ShopListCount(count = shops.size)
            RamenShopSummaries(
                shops = shops,
                onShopClick = onShopOpen,
                itemActionLabel = stringResource(Res.string.shop_notification_settings_title),
                onItemAction = onShopNotificationSettingsOpen,
                modifier = Modifier.padding(horizontal = 15.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscribedShopListContentPreview() {
    val shopProvider = RamenShopPreviewParameterProvider()

    RamapTheme {
        SubscribedShopListContent(
            shops = RamenShops(shopProvider.ramenShopPreviewSamples),
            onShopOpen = {},
            onShopNotificationSettingsOpen = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscribedShopListContentEmptyPreview() {
    RamapTheme {
        SubscribedShopListContent(
            shops = RamenShops(emptyMap()),
            onShopOpen = {},
            onShopNotificationSettingsOpen = {},
        )
    }
}
