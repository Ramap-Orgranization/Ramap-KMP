package com.peto.ramap.designsystem.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor

@Composable
fun AppUnderlineTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Box(
                modifier =
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = 48.dp)
                        .height(48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = label,
                    style = AppTextStyle.B1,
                    color = if (selected) GrayColor.C500 else GrayColor.C300,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                if (selected) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(GrayColor.C500),
                    )
                }
            }
        }
    }
}
