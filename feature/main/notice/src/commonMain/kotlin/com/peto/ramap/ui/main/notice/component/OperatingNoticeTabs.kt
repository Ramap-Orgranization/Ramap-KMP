package com.peto.ramap.ui.main.notice.component

import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.notice.model.OperatingNoticeTab
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.operating_notice_scheduled_tab
import ramap.shared.generated.resources.operating_notice_scheduled_tab_count
import ramap.shared.generated.resources.operating_notice_today_tab
import ramap.shared.generated.resources.operating_notice_today_tab_count

@Composable
internal fun OperatingNoticeTabs(
    selectedTab: OperatingNoticeTab,
    todayCount: Int?,
    scheduledCount: Int?,
    onTabSelected: (OperatingNoticeTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        OperatingNoticeTabItem(
            selected = selectedTab == OperatingNoticeTab.TODAY,
            label =
                if (todayCount == null) {
                    stringResource(Res.string.operating_notice_today_tab)
                } else {
                    stringResource(Res.string.operating_notice_today_tab_count, todayCount)
                },
            onClick = { onTabSelected(OperatingNoticeTab.TODAY) },
        )
        OperatingNoticeTabItem(
            selected = selectedTab == OperatingNoticeTab.SCHEDULED,
            label =
                if (scheduledCount == null) {
                    stringResource(Res.string.operating_notice_scheduled_tab)
                } else {
                    stringResource(Res.string.operating_notice_scheduled_tab_count, scheduledCount)
                },
            onClick = { onTabSelected(OperatingNoticeTab.SCHEDULED) },
        )
    }
    HorizontalDivider(color = GrayColor.C100)
}

@Composable
private fun OperatingNoticeTabItem(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(IntrinsicSize.Max)
                .noRippleClickable(onClick = onClick)
                .padding(top = 16.dp),
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

@Preview(showBackground = true)
@Composable
private fun OperatingNoticeScheduledTabsPreview() {
    RamapTheme {
        Column {
            OperatingNoticeTabs(
                selectedTab = OperatingNoticeTab.SCHEDULED,
                todayCount = 16,
                scheduledCount = 7,
                onTabSelected = {},
            )
        }
    }
}
