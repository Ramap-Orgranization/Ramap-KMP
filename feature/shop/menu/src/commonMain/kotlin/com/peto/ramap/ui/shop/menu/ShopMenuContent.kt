package com.peto.ramap.ui.shop.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.preview.MenuSectionsPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.shop.menu.component.ShopMenuSection
import com.peto.ramap.ui.shop.menu.component.ShopMenuSkeleton
import com.peto.ramap.ui.shop.menu.contract.ShopMenuLoadKey
import com.peto.ramap.ui.shop.menu.contract.ShopMenuUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.menu_load_error_illustration
import ramap.shared.generated.resources.shop_menu_empty_message
import ramap.shared.generated.resources.shop_menu_load_failure_message
import ramap.shared.generated.resources.shop_menu_title
import ramap.shared.generated.resources.shop_menu_updated_at

@Composable
fun ShopMenuContent(
    uiState: ShopMenuUiState,
    onMenuSourceClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        when {
            uiState.isLoading -> {
                ShopMenuSkeleton()
            }

            uiState.hasLoadFailure -> {
                LoadErrorContent(
                    image = Res.drawable.menu_load_error_illustration,
                    title = stringResource(Res.string.shop_menu_load_failure_message),
                    description = stringResource(Res.string.map_shop_detail_error_description),
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
            }

            uiState.menu != null -> {
                ShopMenuContent(
                    sections = uiState.menu.sections,
                    updatedAt = uiState.menu.updatedAt,
                    onMenuSourceClick = onMenuSourceClick,
                )
            }
        }
    }
}

@Composable
fun ShopMenuContent(
    sections: List<MenuSection>,
    updatedAt: String?,
    onMenuSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) {
        AppText(
            text = stringResource(Res.string.shop_menu_empty_message),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
            textAlign = TextAlign.Center,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = stringResource(Res.string.shop_menu_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                modifier = Modifier.weight(1f),
            )
            updatedAt?.takeIf { it.isNotBlank() }?.let { dateStr ->
                AppText(
                    text = stringResource(Res.string.shop_menu_updated_at, dateStr.substringBefore("T")),
                    style = AppTextStyle.C1,
                    color = GrayColor.C400,
                )
            }
        }
        sections.forEach { section ->
            ShopMenuSection(section = section, onMenuSourceClick = onMenuSourceClick)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuLoadingPreview() {
    RamapTheme {
        ShopMenuContent(
            uiState =
                ShopMenuUiState(
                    shopId = "1",
                    loadState = LoadState.loading(ShopMenuLoadKey.Menu),
                ),
            onMenuSourceClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuSuccessSourcePreview() {
    val provider = MenuSectionsPreviewParameterProvider()
    RamapTheme {
        ShopMenuContent(
            uiState =
                ShopMenuUiState(
                    shopId = "1",
                    menu =
                        ShopMenu(
                            sections = provider.sourceMenuSections,
                            updatedAt = "2026-09-02T00:00:00Z",
                        ),
                ),
            onMenuSourceClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuEmptyPreview() {
    RamapTheme {
        ShopMenuContent(
            uiState =
                ShopMenuUiState(
                    shopId = "1",
                    menu = ShopMenu(sections = emptyList(), updatedAt = ""),
                ),
            onMenuSourceClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuErrorPreview() {
    RamapTheme {
        ShopMenuContent(
            uiState =
                ShopMenuUiState(
                    shopId = "1",
                    hasLoadFailure = true,
                ),
            onMenuSourceClick = {},
            onRetry = {},
        )
    }
}
