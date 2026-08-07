package com.peto.ramap.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.MenuCategories
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.menu_category_separator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuCategoryLabels(
    menuCategories: MenuCategories,
    categoryLabel: @Composable (Category) -> String,
    style: AppTextStyle,
    modifier: Modifier = Modifier,
) {
    if (menuCategories.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        menuCategories.forEachIndexed { index, category ->
            AppText(
                text = categoryLabel(category),
                style = style,
                color = GrayColor.C300,
            )
            if (index != menuCategories.lastIndex) {
                AppText(
                    text = stringResource(Res.string.menu_category_separator),
                    style = AppTextStyle.B1,
                    color = GrayColor.C300,
                )
            }
        }
    }
}
