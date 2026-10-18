package com.peto.ramap.ui.shop.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.ui.shop.menu.ShopMenuContent
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailTab
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.laduck_error_crying
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.map_shop_detail_error_title

@Composable
internal fun RegionLoadError(onRetry: () -> Unit) {
    LoadErrorContent(
        image = Res.drawable.laduck_error_crying,
        title = stringResource(Res.string.map_shop_detail_error_title),
        description = stringResource(Res.string.map_shop_detail_error_description),
        onRetry = onRetry,
        modifier = Modifier.fillMaxWidth(),
        compact = true,
    )
}

internal fun shopDetailSections(
    scope: LazyListScope,
    uiState: ShopDetailContentUiState,
    actions: ShopDetailActions,
    selectedTab: ShopDetailTab,
    onTabSelected: (ShopDetailTab) -> Unit,
    reviewItems: LazyListScope.() -> Unit,
) {
    scope.item(key = "shop_detail_tabs", contentType = "tabs") {
        OverviewTabRow(
            menuCount = uiState.menuState?.menu?.itemCount ?: 0,
            reviewCount = uiState.reviewState.reviewCount,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
    when (selectedTab) {
        ShopDetailTab.MENU -> {
            scope.item(key = "shop_detail_menu", contentType = "menu") {
                ShopMenuContent(
                    uiState = uiState.menuState ?: ShopMenuUiState(),
                    onRetry = actions.onMenuRetry,
                    onMenuSourceClick = actions.externalLinkActions.onExternalLinkClick,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        ShopDetailTab.REVIEW -> scope.reviewItems()
    }
}
