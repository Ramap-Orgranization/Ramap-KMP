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
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.ui.extension.stringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MenuCategoryFilterRow(
    selectedCategories: Set<Category>,
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
        Category.entries.forEach { category ->
            CategoryFilterChip(
                label = stringResource(category.stringResource),
                selected = category in selectedCategories,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}
