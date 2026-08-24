package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
internal fun AdminNoticeTypeSelector(
    selectedNoticeType: OperatingNoticeType?,
    onNoticeTypeSelected: (OperatingNoticeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OperatingNoticeType.entries.forEach { noticeType ->
            FilterChip(
                selected = selectedNoticeType == noticeType,
                onClick = { onNoticeTypeSelected(noticeType) },
                label = {
                    AppText(
                        text = noticeType.label(),
                        style = AppTextStyle.B4,
                        color = if (selectedNoticeType == noticeType) GrayColor.C500 else GrayColor.C300,
                    )
                },
                shape = RoundedCornerShape(100.dp),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrayColor.C050,
                        selectedLabelColor = GrayColor.C500,
                        containerColor = GrayColor.C050.copy(alpha = 0f),
                        labelColor = GrayColor.C300,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedNoticeType == noticeType,
                        borderColor = GrayColor.C200,
                        selectedBorderColor = GrayColor.C200,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp,
                    ),
            )
        }
    }
}

@Composable
private fun OperatingNoticeType.label(): String =
    when (this) {
        OperatingNoticeType.OPERATING_NOTICE,
        OperatingNoticeType.TEMPORARY_CLOSURE,
        OperatingNoticeType.EARLY_CLOSING,
        OperatingNoticeType.LATE_OPENING,
        -> composeStringResource(ShopOperatingNoticeResourceMapper.typeLabel(this))
    }

@Preview(showBackground = true)
@Composable
private fun AdminNoticeTypeSelectorPreview() {
    RamapTheme {
        AdminNoticeTypeSelector(
            selectedNoticeType = OperatingNoticeType.TEMPORARY_CLOSURE,
            onNoticeTypeSelected = {},
        )
    }
}
