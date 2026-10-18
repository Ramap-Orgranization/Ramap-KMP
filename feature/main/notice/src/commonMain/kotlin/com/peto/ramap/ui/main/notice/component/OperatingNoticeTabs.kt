package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_scheduled_tab
import ramap.shared.generated.resources.operating_notice_scheduled_tab_count
import ramap.shared.generated.resources.operating_notice_today_tab
import ramap.shared.generated.resources.operating_notice_today_tab_count

@Composable
internal fun OperatingNoticeTabs(
    scheduledSelected: Boolean,
    todayCount: Int?,
    scheduledCount: Int?,
    onTabSelected: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        for (scheduled in listOf(false, true)) {
            val selected = scheduledSelected == scheduled
            val count = if (scheduled) scheduledCount else todayCount
            val label =
                if (count == null) {
                    stringResource(if (scheduled) Res.string.operating_notice_scheduled_tab else Res.string.operating_notice_today_tab)
                } else {
                    stringResource(if (scheduled) Res.string.operating_notice_scheduled_tab_count else Res.string.operating_notice_today_tab_count, count)
                }
            val interactionSource = remember { MutableInteractionSource() }
            Column(
                modifier =
                    Modifier
                        .width(IntrinsicSize.Max)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(scheduled) },
                        ).padding(top = 16.dp),
            ) {
                AppText(
                    text = label,
                    modifier = Modifier.padding(horizontal = 2.dp),
                    style = AppTextStyle.B1,
                    color = if (selected) GrayColor.C500 else GrayColor.C300,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = if (selected) CommonColor.Black else CommonColor.White,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            ),
                )
            }
        }
    }
    HorizontalDivider(color = GrayColor.C100)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OperatingNoticeScheduledTabsPreview() {
    RamapTheme {
        Column {
            OperatingNoticeTabs(
                scheduledSelected = true,
                todayCount = 16,
                scheduledCount = 7,
                onTabSelected = {},
            )
        }
    }
}
