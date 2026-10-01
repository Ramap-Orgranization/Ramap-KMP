package com.peto.ramap.ui.main.map.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.map.shop.model.ShopDetailTab
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_menu_title
import ramap.shared.generated.resources.shop_review_title

@Composable
internal fun ShopDetailTabRow(
    menuCount: Int,
    reviewCount: Int,
    selectedTab: ShopDetailTab,
    onTabSelected: (ShopDetailTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShopDetailTabItem(
            title = stringResource(Res.string.shop_menu_title),
            count = menuCount,
            selected = selectedTab == ShopDetailTab.MENU,
            onClick = { onTabSelected(ShopDetailTab.MENU) },
            modifier = Modifier.weight(1f),
        )
        ShopDetailTabItem(
            title = stringResource(Res.string.shop_review_title),
            count = reviewCount,
            selected = selectedTab == ShopDetailTab.REVIEW,
            onClick = { onTabSelected(ShopDetailTab.REVIEW) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShopDetailTabItem(
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = title,
                    style = AppTextStyle.B1,
                    color = if (selected) GrayColor.C500 else GrayColor.C400,
                    textAlign = TextAlign.Center,
                )
                if (selected) {
                    Box(
                        modifier =
                            Modifier
                                .background(
                                    color = GrayColor.C500,
                                    shape = RoundedCornerShape(999.dp),
                                ).padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            text = "$count",
                            style = AppTextStyle.C1,
                            color = CommonColor.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    AppText(
                        text = "$count",
                        style = AppTextStyle.B1,
                        color = GrayColor.C200,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (selected) {
                HorizontalDivider(
                    thickness = 2.dp,
                    color = GrayColor.C500,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopDetailTabRowPreview() {
    RamapTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShopDetailTabRow(
                menuCount = 12,
                reviewCount = 34,
                selectedTab = ShopDetailTab.MENU,
                onTabSelected = {},
            )
            ShopDetailTabRow(
                menuCount = 12,
                reviewCount = 34,
                selectedTab = ShopDetailTab.REVIEW,
                onTabSelected = {},
            )
        }
    }
}
