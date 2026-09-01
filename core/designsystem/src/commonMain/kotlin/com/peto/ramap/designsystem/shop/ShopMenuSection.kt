package com.peto.ramap.designsystem.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.badge.NewsBadge
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.menu.Menu
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.model.menu.Price
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun ShopMenuSection(
    section: MenuSection,
    onMenuSourceClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NewsBadge(
            text = section.title,
            textStyle = AppTextStyle.B1,
            contentColor = CommonColor.White,
            containerColor = GrayColor.C400,
        )
        section.description?.let { description ->
            AppText(text = description, style = AppTextStyle.B2, color = GrayColor.C300)
        }
        section.items.forEachIndexed { index, item ->
            MenuRow(item = item, onSourceClick = onMenuSourceClick)
            if (index < section.items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    thickness = 1.dp,
                    color = GrayColor.C100,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShopMenuSectionPreview() {
    RamapTheme {
        ShopMenuSection(
            section =
                MenuSection(
                    id = "1",
                    title = "대표 메뉴",
                    description = "가장 인기 있는 메뉴입니다.",
                    displayOrder = 0,
                    items =
                        Menus(
                            listOf(
                                Menu(
                                    id = "1",
                                    name = "시오라멘",
                                    priceKrw = Price(10000),
                                    description = "깔끔한 소금 베이스의 라멘",
                                    imageUrl = null,
                                    displayOrder = 0,
                                    isRepresentative = true,
                                ),
                                Menu(
                                    id = "2",
                                    name = "쇼유라멘",
                                    priceKrw = Price(11000),
                                    description = "깊은 맛의 간장 베이스 라멘",
                                    imageUrl = null,
                                    displayOrder = 1,
                                ),
                            ),
                        ),
                ),
            onMenuSourceClick = {},
        )
    }
}
