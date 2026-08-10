package com.peto.ramap.ui.main.ranking.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.ranking.contract.RankingUiState
import com.peto.ramap.ui.resource.area.AreaFilterResourceMapper
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.designsystem.resource.format
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_categories

@Composable
internal fun RankingFilters(
    uiState: RankingUiState,
    onAreaClick: () -> Unit,
    onCategoryToggled: (Category) -> Unit,
    onAllCategoriesSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier =
            modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            AreaFilterChip(
                label = AreaFilterResourceMapper.label(uiState.areaFilter).format(),
                onClick = onAreaClick,
            )
        }
        item {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 8.dp)
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(GrayColor.C100),
            )
        }
        item {
            CategoryFilterChip(
                label = stringResource(Res.string.ranking_all_categories),
                selected = uiState.selectedCategories.isEmpty(),
                onClick = onAllCategoriesSelected,
                style = AppTextStyle.L3,
            )
        }
        items(Category.entries) { category ->
            CategoryFilterChip(
                label = stringResource(CategoryResourceMapper.label(category)),
                selected = category in uiState.selectedCategories,
                onClick = { onCategoryToggled(category) },
                style = AppTextStyle.L3,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RankingFiltersPreview() {
    RamapTheme {
        RankingFilters(
            uiState =
                RankingUiState(
                    areaFilter = AreaFilter.Province(AdministrativeArea.SEOUL),
                    selectedCategories = setOf(Category.TONKOTSU, Category.SHOYU),
                ),
            onAreaClick = {},
            onCategoryToggled = {},
            onAllCategoriesSelected = {},
        )
    }
}
