package com.peto.ramap.ui.main.event.detail.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_cancellation_notice_title
import ramap.shared.generated.resources.ic_warning

@Composable
internal fun EventCancellationNotice(
    reason: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SystemColor.Warning.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, SystemColor.Warning.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_warning),
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                )
                AppText(
                    text = stringResource(Res.string.event_cancellation_notice_title),
                    style = AppTextStyle.T1,
                    color = SystemColor.Warning,
                )
            }
            AppText(
                text = reason,
                style = AppTextStyle.B1,
                color = GrayColor.C500,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventCancellationNoticePreview() {
    RamapTheme {
        EventCancellationNotice(
            reason = "이용 확인 부탁드립니다. 연휴 기간중 냉유자츠케멘의 판매가 어렵습니다🙏",
            modifier = Modifier.padding(16.dp),
        )
    }
}
