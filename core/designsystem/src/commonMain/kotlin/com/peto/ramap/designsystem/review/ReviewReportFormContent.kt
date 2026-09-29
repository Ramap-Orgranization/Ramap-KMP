package com.peto.ramap.designsystem.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.resource.reportReasonResource
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_info
import ramap.shared.generated.resources.review_report_description
import ramap.shared.generated.resources.review_report_details
import ramap.shared.generated.resources.review_report_details_required
import ramap.shared.generated.resources.review_report_policy_body
import ramap.shared.generated.resources.review_report_policy_title

@Composable
fun ReviewReportFormContent(
    selectedReason: ReportReason?,
    onReasonSelected: (ReportReason) -> Unit,
    details: String,
    onDetailsChanged: (String) -> Unit,
    isActing: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
        ) {
            ReportPolicyNotice()
            AppText(
                text = stringResource(Res.string.review_report_description),
                modifier = Modifier.padding(top = 20.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C500,
            )
            ReportReason.entries.forEach { reason ->
                ReportReasonRow(
                    reason = reason,
                    selected = selectedReason == reason,
                    enabled = !isActing,
                    onClick = { onReasonSelected(reason) },
                )
            }
        }
        ReportDetailsField(
            details = details,
            onDetailsChanged = onDetailsChanged,
            maxLength = selectedReason?.maxDetailsLength ?: ReportReason.MAX_DETAILS_LENGTH,
            detailsRequired = selectedReason?.requiresDetails == true,
            enabled = !isActing,
        )
    }
}

@Composable
private fun ReportPolicyNotice() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ChromaticColor.Blue050)
                .border(BorderStroke(1.dp, ChromaticColor.Blue100), RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_info),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ChromaticColor.Blue500),
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppText(
                text = stringResource(Res.string.review_report_policy_title),
                style = AppTextStyle.C1,
                color = ChromaticColor.Blue500,
            )
            AppText(
                text = stringResource(Res.string.review_report_policy_body),
                style = AppTextStyle.C2,
                color = ChromaticColor.Blue500,
            )
        }
    }
}

@Composable
private fun ReportReasonRow(
    reason: ReportReason,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clip(rowShape)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(16.dp)
                    .border(
                        width = if (selected) 5.dp else 1.dp,
                        color = if (selected) ChromaticColor.Blue400 else GrayColor.C300,
                        shape = CircleShape,
                    ),
        )
        AppText(
            text = stringResource(reportReasonResource(reason)),
            modifier = Modifier.padding(start = 12.dp).weight(1f),
            style = AppTextStyle.B1,
            color = GrayColor.C500,
        )
    }
}

@Composable
private fun ReportDetailsField(
    details: String,
    onDetailsChanged: (String) -> Unit,
    maxLength: Int,
    detailsRequired: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, GrayColor.C200, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text =
                stringResource(
                    if (detailsRequired) {
                        Res.string.review_report_details_required
                    } else {
                        Res.string.review_report_details
                    },
                    maxLength,
                ),
            style = AppTextStyle.B3,
            color = GrayColor.C300,
        )
        BasicTextField(
            value = details,
            onValueChange = { onDetailsChanged(it.take(maxLength)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            enabled = enabled,
            textStyle = LocalAppTypography.current.b1.copy(color = GrayColor.C500),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewReportFormContentPreview() {
    RamapTheme {
        ReviewReportFormContent(
            selectedReason = ReportReason.SPAM,
            onReasonSelected = {},
            details = "같은 광고 문구가 반복됩니다.",
            onDetailsChanged = {},
            isActing = false,
            modifier = Modifier.height(540.dp).padding(12.dp),
        )
    }
}
