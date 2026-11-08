package com.peto.ramap.ui.shop.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.switch.AppSwitch
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notification.NotificationType
import com.peto.ramap.domain.model.notification.NotificationTypeId
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme

@Composable
internal fun NotificationTypeChoice(
    type: NotificationType,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clip(shape)
                .background(CommonColor.White)
                .border(1.dp, GrayColor.C100, shape)
                .toggleable(
                    value = selected,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                ).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AppText(
                text = type.title,
                style = AppTextStyle.T2,
                color = if (enabled) GrayColor.C500 else GrayColor.C300,
            )
            AppText(
                text = type.description,
                style = AppTextStyle.C1,
                color = GrayColor.C300,
            )
        }
        AppSwitch(
            checked = selected,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationTypeChoicePreview() {
    RamapTheme {
        NotificationTypeChoice(
            type =
                NotificationType(
                    id = NotificationTypeId("event"),
                    title = "이벤트 소식",
                    description = "매장의 새로운 이벤트 알림을 받습니다.",
                    displayOrder = 0,
                ),
            selected = true,
            enabled = true,
            onToggle = {},
        )
    }
}
