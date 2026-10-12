package com.peto.ramap.designsystem.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor

@Composable
fun AppSegmentedTabs(
    tabs: List<AppSegmentedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GrayColor.C050)
                .selectableGroup()
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            val textColor = if (selected) GrayColor.C500 else GrayColor.C300
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) CommonColor.White else GrayColor.C050)
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ).heightIn(min = 44.dp)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(text = tab.label, style = if (selected) AppTextStyle.B1 else AppTextStyle.B2, color = textColor)
                tab.count?.let { count ->
                    val countModifier =
                        if (tab.hasCountBadge) {
                            Modifier.clip(RoundedCornerShape(12.dp)).background(ChromaticColor.Orange050).padding(horizontal = 5.dp, vertical = 1.dp)
                        } else {
                            Modifier
                        }
                    AppText(
                        text = count,
                        style = AppTextStyle.C1,
                        color = if (selected || tab.hasCountBadge) ChromaticColor.Orange400 else GrayColor.C300,
                        modifier = countModifier,
                    )
                }
            }
        }
    }
}
