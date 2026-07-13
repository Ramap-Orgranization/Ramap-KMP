package com.peto.ramap.ui.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.peto.ramap.core.extension.stringResource
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.Category
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
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
            MenuCategoryFilterChip(
                label = stringResource(category.stringResource),
                selected = category in selectedCategories,
                onClick = { onCategoryClick(category) },
            )
        }
    }
}

@Composable
private fun MenuCategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) GrayColor.C500 else GrayColor.C050,
        border =
            BorderStroke(
                width = 1.dp,
                color = if (selected) GrayColor.C500 else CommonColor.White,
            ),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier,
        onClick = onClick,
        shadowElevation = 2.dp,
    ) {
        AppText(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = AppTextStyle.C1,
            color = if (selected) CommonColor.White else GrayColor.C500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
