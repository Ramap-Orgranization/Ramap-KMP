package com.peto.ramap.designsystem.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.BrandColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.feed_filter_all
import ramap.shared.generated.resources.feed_filter_following
import ramap.shared.generated.resources.feed_filter_saved_shops

@Composable
fun AppChipTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabShape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier =
                    Modifier
                        .background(
                            color = if (isSelected) BrandColor.Primary else BrandColor.SubtleFill,
                            shape = tabShape,
                        ).semantics { this.selected = isSelected }
                        .noRippleClickable(
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ).sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = label,
                    style = AppTextStyle.B1,
                    color = if (isSelected) BrandColor.OnSelectionFill else BrandColor.SecondPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppChipTabsPreview() {
    val labels =
        listOf(
            stringResource(Res.string.feed_filter_all),
            stringResource(Res.string.feed_filter_saved_shops),
            stringResource(Res.string.feed_filter_following),
        )
    RamapTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (selectedIndex in labels.indices) {
                AppChipTabs(
                    labels = labels,
                    selectedIndex = selectedIndex,
                    onSelect = {},
                )
            }
        }
    }
}
