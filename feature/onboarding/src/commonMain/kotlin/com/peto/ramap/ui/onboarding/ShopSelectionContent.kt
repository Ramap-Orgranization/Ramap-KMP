package com.peto.ramap.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.component.AppSearchBar
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.component.RamenShopSummary
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.onboarding.OnboardingBookmarks
import com.peto.ramap.extension.noRippleToggleable
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.InstagramColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.onboarding.component.ShopSelectionNextPageFooter
import com.peto.ramap.ui.onboarding.component.ShopSelectionSkeleton
import com.peto.ramap.ui.onboarding.contract.ShopSelectionIntent
import com.peto.ramap.ui.onboarding.contract.ShopSelectionLoadKey
import com.peto.ramap.ui.onboarding.contract.ShopSelectionUiState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_star_filled
import ramap.shared.generated.resources.menu_load_error_illustration
import ramap.shared.generated.resources.onboarding_shop_confirm
import ramap.shared.generated.resources.onboarding_shop_empty
import ramap.shared.generated.resources.onboarding_shop_load_failure_description
import ramap.shared.generated.resources.onboarding_shop_load_failure_title
import ramap.shared.generated.resources.onboarding_shop_remaining
import ramap.shared.generated.resources.onboarding_shop_title
import ramap.shared.generated.resources.personalization_load_failure_title
import ramap.shared.generated.resources.search_result_empty_message

@Composable
internal fun ShopSelectionContent(
    uiState: ShopSelectionUiState,
    onIntent: (ShopSelectionIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = CommonColor.White)
                .safeDrawingPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 15.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "heading") {
                AppText(
                    text = stringResource(Res.string.onboarding_shop_title),
                    style = AppTextStyle.H3,
                    color = GrayColor.C500,
                    modifier = Modifier.semantics { heading() },
                )
            }
            item(key = "search") {
                AppSearchBar(
                    query = uiState.query,
                    onQueryChange = { onIntent(ShopSelectionIntent.QueryChanged(it)) },
                    onSearch = { onIntent(ShopSelectionIntent.Search) },
                )
            }
            when {
                uiState.loadState.isLoading(ShopSelectionLoadKey.Personalization) || uiState.isLoadingShops ->
                    item(key = "loading") { ShopSelectionSkeleton() }

                uiState.hasPersonalizationError || uiState.hasShopsError ->
                    item(key = "error") {
                        LoadErrorContent(
                            image = Res.drawable.menu_load_error_illustration,
                            title = stringResource(if (uiState.hasPersonalizationError) Res.string.personalization_load_failure_title else Res.string.onboarding_shop_load_failure_title),
                            description = stringResource(Res.string.onboarding_shop_load_failure_description),
                            onRetry = { onIntent(if (uiState.hasPersonalizationError) ShopSelectionIntent.RetryPersonalization else ShopSelectionIntent.RetryShops) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        )
                    }

                uiState.shops.isEmpty() ->
                    item(key = "empty") {
                        AppText(
                            text = stringResource(if (uiState.isSearching) Res.string.search_result_empty_message else Res.string.onboarding_shop_empty),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                            modifier = Modifier.padding(vertical = 32.dp),
                        )
                    }

                else -> {
                    items(
                        items = uiState.shops.values.toList(),
                        key = { "shop-${it.id}" },
                    ) { shop ->
                        val saved = shop.id in uiState.bookmarks
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .noRippleToggleable(
                                        value = saved,
                                        enabled = uiState.canChangeBookmarks,
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
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    if (uiState.showsNextPageFooter) {
                        item(key = "next-page") {
                            ShopSelectionNextPageFooter(
                                uiState = uiState,
                                onLoadNext = { onIntent(ShopSelectionIntent.LoadNextPage) },
                                onRetry = { onIntent(ShopSelectionIntent.RetryNextPage) },
                            )
                        }
                    }
                }
            }
        }
        AppButton(
            text =
                if (uiState.bookmarks.canComplete) {
                    stringResource(Res.string.onboarding_shop_confirm)
                } else {
                    stringResource(Res.string.onboarding_shop_remaining, uiState.bookmarks.savedCount)
                },
            onClick = { onIntent(ShopSelectionIntent.Complete) },
            enabled = uiState.canComplete || uiState.isCompleting,
            isLoading = uiState.isCompleting,
            disabledBackgroundColor = if (uiState.bookmarks.canComplete) GrayColor.C500 else GrayColor.C100,
            disabledTextColor = if (uiState.bookmarks.canComplete) CommonColor.White else GrayColor.C300,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 15.dp)
                    .padding(horizontal = 15.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSelectionContentPreview() {
    val shops = RamenShopsPreviewParameterProvider().values.first()
    RamapTheme {
        ShopSelectionContent(
            uiState = ShopSelectionUiState(suggestedShops = shops, hasPersonalization = true, bookmarks = OnboardingBookmarks(shops.keys.take(2).toSet())),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSelectionLoadingPreview() {
    RamapTheme {
        ShopSelectionContent(
            uiState = ShopSelectionUiState(loadState = LoadState.loading(ShopSelectionLoadKey.Suggestions)),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopSelectionLoadFailurePreview() {
    RamapTheme {
        ShopSelectionContent(
            uiState = ShopSelectionUiState(hasPersonalization = true, hasSuggestionsError = true),
            onIntent = {},
        )
    }
}
