package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.preview.MenuSectionsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_menu_title

@Composable
internal fun ShopMenuContent(
    sections: List<MenuSection>,
    onMenuSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppText(
            text = stringResource(Res.string.shop_menu_title),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
        )
        sections.forEach { section ->
            ShopMenuSection(section = section, onMenuSourceClick = onMenuSourceClick)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuContentPreview() {
    RamapTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShopMenuContent(
                sections = MenuSectionsPreviewParameterProvider().defaultMenuSections,
                onMenuSourceClick = {},
            )
            ShopMenuContent(
                sections = MenuSectionsPreviewParameterProvider().highlightedMenuSections,
                onMenuSourceClick = {},
            )
            ShopMenuContent(
                sections = MenuSectionsPreviewParameterProvider().sourceMenuSections,
                onMenuSourceClick = {},
            )
        }
    }
}
