package com.peto.ramap.debug.admin.ui.registration.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.debug.admin.R
import com.peto.ramap.debug.admin.data.model.AdminDraft
import com.peto.ramap.debug.admin.ui.registration.formatDateRange
import com.peto.ramap.debug.admin.ui.registration.toOperatingNoticeType
import com.peto.ramap.designsystem.resource.operatingnotice.ShopOperatingNoticeResourceMapper
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
internal fun AdminDraftPreview(
    draft: AdminDraft,
    modifier: Modifier = Modifier,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, GrayColor.C200, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = stringResource(R.string.admin_registration_preview_title),
            style = AppTextStyle.T2,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(R.string.admin_registration_title_label),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
        AdminTitleField(draft.title, onTitleChanged)
        PreviewRow(stringResource(R.string.admin_registration_shop), draft.shopName)
        PreviewRow(
            stringResource(R.string.admin_registration_collaborators),
            draft.participants.joinToString(", ") { participant -> participant.name },
        )
        PreviewRow(stringResource(R.string.admin_registration_source), draft.sourceUrl)
        PreviewRow(
            stringResource(R.string.admin_registration_date_range),
            formatDateRange(draft.startDate, draft.endDate),
        )
        PreviewRow(
            stringResource(R.string.admin_registration_detailed_classification),
            draft.noticeType?.let { toOperatingNoticeType(it)?.label() },
        )
        PreviewRow(stringResource(R.string.admin_registration_start_time), draft.startTime)
        PreviewRow(stringResource(R.string.admin_registration_end_time), draft.endTime)
        AppText(
            text = stringResource(R.string.admin_registration_description),
            style = AppTextStyle.B1,
            color = GrayColor.C400,
        )
        OutlinedTextField(
            value = draft.description.orEmpty(),
            onValueChange = onDescriptionChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 10,
            placeholder = {
                Text(
                    text = stringResource(R.string.admin_registration_preview_empty),
                    color = GrayColor.C200,
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = previewTextFieldColors(),
        )
        if (draft.uncertainties.isNotEmpty()) {
            AppText(
                text = draft.uncertainties.joinToString("\n"),
                modifier = Modifier.fillMaxWidth(),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
            )
        }
    }
}

@Composable
internal fun AdminTitleField(
    title: String?,
    onTitleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = title.orEmpty(),
        onValueChange = onTitleChanged,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = stringResource(R.string.admin_registration_title_placeholder),
                color = GrayColor.C200,
            )
        },
        shape = RoundedCornerShape(10.dp),
        colors = previewTextFieldColors(),
    )
}

@Composable
private fun previewTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GrayColor.C200,
        unfocusedBorderColor = GrayColor.C200,
        cursorColor = GrayColor.C500,
    )

@Composable
private fun PreviewRow(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppText(
            text = label,
            style = AppTextStyle.B2,
            color = GrayColor.C300,
        )
        AppText(
            text = value,
            modifier = Modifier.weight(1f),
            style = AppTextStyle.B2,
            color = GrayColor.C400,
            overflow = TextOverflow.Clip,
        )
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
private fun AdminDraftPreviewPreview() {
    RamapTheme {
        AdminDraftPreview(
            draft =
                AdminDraft(
                    shopName = "멘야준",
                    sourceUrl = "https://instagram.com/p/...",
                    startDate = "2024-05-10",
                    endDate = "2024-05-10",
                    noticeType = "TEMPORARY_CLOSURE",
                    startTime = "11:00",
                    endTime = "21:00",
                    description = "내부 공사로 인한 임시 휴무입니다.",
                    evidencePath = "evidence/path.jpg",
                    uncertainties = listOf("정확한 영업 재개일은 미정입니다."),
                ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
