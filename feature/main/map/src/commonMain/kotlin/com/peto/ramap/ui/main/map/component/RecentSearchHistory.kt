package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peto.ramap.designsystem.component.RamenShopSummaries
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.preview.RamenShopsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.recent_search_clear_all
import ramap.shared.generated.resources.recent_search_delete_action
import ramap.shared.generated.resources.recent_searches_title
import ramap.shared.generated.resources.recent_viewed_shops_title
import ramap.shared.generated.resources.search_bar_search_icon

@Composable
internal fun RecentSearchHistory(
    searches: List<String>,
    viewedShops: RamenShops,
    onSearchSelected: (String) -> Unit,
    onSearchDeleted: (String) -> Unit,
    onSearchesCleared: () -> Unit,
    onViewedShopSelected: (RamenShop) -> Unit,
    categoryLabel: @Composable (Category) -> String,
    modifier: Modifier = Modifier,
) {
    if (searches.isEmpty() && viewedShops.isEmpty()) return
    val searchIconDescription = stringResource(Res.string.search_bar_search_icon)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (searches.isNotEmpty()) {
            HistoryHeader(
                title = stringResource(Res.string.recent_searches_title),
                action = stringResource(Res.string.recent_search_clear_all),
                onAction = onSearchesCleared,
                modifier = Modifier.padding(top = 15.dp),
            )
            searches.forEach { query ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp)
                            .noRippleClickable { onSearchSelected(query) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "⌕",
                        modifier =
                            Modifier.semantics {
                                contentDescription = searchIconDescription
                            },
                        fontSize = 25.sp,
                        color = GrayColor.C500,
                    )
                    AppText(
                        text = query,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 5.dp),
                        style = AppTextStyle.B2,
                        color = GrayColor.C500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.recent_search_delete_action),
                        tint = GrayColor.C400,
                        modifier = Modifier.noRippleClickable(onClick = { onSearchDeleted(query) }),
                    )
                }
            }
        }
        if (viewedShops.isNotEmpty()) {
            HistoryHeader(
                title = stringResource(Res.string.recent_viewed_shops_title),
                modifier = Modifier.padding(top = 5.dp),
            )
            RamenShopSummaries(
                shops = viewedShops,
                onShopClick = onViewedShopSelected,
                categoryLabel = categoryLabel,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

@Composable
private fun HistoryHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(
            text = title,
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )
        if (action != null && onAction != null) {
            AppText(
                text = action,
                style = AppTextStyle.B2,
                color = GrayColor.C300,
                modifier = Modifier.noRippleClickable(onClick = onAction),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentSearchHistoryPreview(
    @PreviewParameter(RamenShopsPreviewParameterProvider::class) viewedShops: RamenShops,
) {
    RamapTheme {
        RecentSearchHistory(
            searches = listOf("돈코츠 라멘", "이에케 라멘", "마제소바"),
            viewedShops = viewedShops,
            onSearchSelected = {},
            onSearchDeleted = {},
            onSearchesCleared = {},
            onViewedShopSelected = {},
            categoryLabel = { it.name },
        )
    }
}
