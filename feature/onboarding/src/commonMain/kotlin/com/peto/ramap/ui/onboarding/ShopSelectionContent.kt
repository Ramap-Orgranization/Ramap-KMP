package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.AppSearchBar
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.contract.ShopSelectionUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.data_load_failure_message
import ramap.shared.generated.resources.ic_star_filled
import ramap.shared.generated.resources.laduck_error_confused
import ramap.shared.generated.resources.onboarding_shop_confirm
import ramap.shared.generated.resources.onboarding_shop_count
import ramap.shared.generated.resources.onboarding_shop_description
import ramap.shared.generated.resources.onboarding_shop_empty
import ramap.shared.generated.resources.onboarding_shop_remaining
import ramap.shared.generated.resources.onboarding_shop_suggestions
import ramap.shared.generated.resources.onboarding_shop_title
import ramap.shared.generated.resources.personalization_load_failure_title
import ramap.shared.generated.resources.search_result_empty_message

@Composable
internal fun ShopSelectionContent(
    state: ShopSelectionUiState,
    onIntent: (ShopSelectionIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CommonColor.White)
                .safeDrawingPadding()
                .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppText(
                        text = stringResource(Res.string.onboarding_shop_title),
                        style = AppTextStyle.H3,
                        color = GrayColor.C500,
                        modifier = Modifier.semantics { heading() },
                    )
                    AppText(
                        text = stringResource(Res.string.onboarding_shop_description),
                        style = AppTextStyle.B2,
                        color = GrayColor.C300,
                    )
                }
            }
            item(key = "search") {
                AppSearchBar(
                    query = state.query,
                    onQueryChange = { onIntent(ShopSelectionIntent.QueryChanged(it)) },
                    onSearch = { onIntent(ShopSelectionIntent.Search) },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            when {
                state.loadState.isLoading(ShopSelectionLoadKey.Personalization) || state.isLoadingShops ->
                    item(key = "loading") {
                        RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().height(160.dp))
                    }
                state.hasPersonalizationError || state.hasShopsError ->
                    item(key = "error") {
                        LoadErrorContent(
                            image = Res.drawable.laduck_error_confused,
                            title = stringResource(if (state.hasPersonalizationError) Res.string.personalization_load_failure_title else Res.string.data_load_failure_message),
                            onRetry = { onIntent(if (state.hasPersonalizationError) ShopSelectionIntent.RetryPersonalization else ShopSelectionIntent.RetryShops) },
                            modifier = Modifier.fillMaxWidth(),
                            compact = true,
                        )
                    }
                state.shops.isEmpty() ->
                    item(key = "empty") {
                        AppText(
                            text = stringResource(if (state.isSearching) Res.string.search_result_empty_message else Res.string.onboarding_shop_empty),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }
                else -> {
                    if (!state.isSearching) {
                        item(key = "suggestions") {
                            AppText(
                                text = stringResource(Res.string.onboarding_shop_suggestions),
                                style = AppTextStyle.T2,
                                color = GrayColor.C500,
                            )
                        }
                    }
                    items(items = state.shops.values.toList(), key = { "shop-${it.id}" }) { shop ->
                        val saved = shop.id in state.bookmarks
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().heightIn(min = 80.dp).toggleable(
                                    value = saved,
                                    enabled = state.canChangeBookmarks,
                                    role = Role.Checkbox,
                                    onValueChange = { onIntent(ShopSelectionIntent.ToggleBookmark(shop.id)) },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RamenShopSummary(shop = shop, modifier = Modifier.weight(1f))
                            Icon(
                                painter = painterResource(Res.drawable.ic_star_filled),
                                contentDescription = null,
                                tint = if (saved) InstagramColor.Pink else GrayColor.C200,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }
        ShopSelectionFooter(state = state, onConfirm = { onIntent(ShopSelectionIntent.Complete) })
    }
}

@Composable
private fun ShopSelectionFooter(
    state: ShopSelectionUiState,
    onConfirm: () -> Unit,
) {
    HorizontalDivider(color = GrayColor.C100)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.actionError?.let { error ->
            AppText(
                text = stringResource(error),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        AppText(
            text = stringResource(Res.string.onboarding_shop_count, state.bookmarks.savedCount),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        AppButton(
            text =
                if (state.bookmarks.canComplete) {
                    stringResource(Res.string.onboarding_shop_confirm)
                } else {
                    stringResource(Res.string.onboarding_shop_remaining, state.bookmarks.remainingCount)
                },
            onClick = onConfirm,
            enabled = state.canComplete || state.isCompleting,
            isLoading = state.isCompleting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ShopSelectionContentPreview() {
    val shops = RamenShopsPreviewParameterProvider().values.first()
    RamapTheme {
        ShopSelectionContent(
            state = ShopSelectionUiState(suggestedShops = shops, hasPersonalization = true, bookmarks = OnboardingBookmarks(shops.keys.take(2).toSet())),
            onIntent = {},
        )
    }
}

@Preview(widthDp = 320, heightDp = 480)
@Composable
private fun ShopSelectionContentCompactPreview() {
    RamapTheme {
        ShopSelectionContent(state = ShopSelectionUiState(), onIntent = {})
    }
}
