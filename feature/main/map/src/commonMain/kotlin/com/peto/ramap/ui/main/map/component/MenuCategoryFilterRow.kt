package com.peto.ramap.ui.main.map.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.CategoryFilterChip
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.theme.AppTextStyle
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MenuCategoryFilterRow(
    selectedFilter: RamenShopFilter,
    now: LocalDateTime,
    hoursFilterResultCount: (ShopHoursFilter) -> Int,
    onHoursFilterOpened: () -> Unit,
    onHoursFilterApplied: (ShopHoursFilter) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        HoursFilterMenu(
            selected = selectedFilter.hours,
            now = now,
            resultCount = hoursFilterResultCount,
            onOpen = onHoursFilterOpened,
            onApply = onHoursFilterApplied,
        )
        Category.entries.forEach { category ->
            CategoryFilterChip(
                label = stringResource(CategoryResourceMapper.label(category)),
                selected = category in selectedFilter,
                onClick = { onCategoryClick(category) },
                style = AppTextStyle.B4,
            )
        }
    }
}
