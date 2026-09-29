package com.peto.ramap.ui.profile.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_reason_harassment
import ramap.shared.generated.resources.review_reason_hate
import ramap.shared.generated.resources.review_reason_other
import ramap.shared.generated.resources.review_reason_privacy
import ramap.shared.generated.resources.review_reason_sexual
import ramap.shared.generated.resources.review_reason_spam
import ramap.shared.generated.resources.review_reason_violence
import ramap.shared.generated.resources.review_report_description
import ramap.shared.generated.resources.review_report_details

@Composable
internal fun ReviewReportFormContent(
    selectedReason: ReportReason?,
    onReasonSelected: (ReportReason) -> Unit,
    details: String,
    onDetailsChanged: (String) -> Unit,
    isActing: Boolean,
    actionFailed: Boolean,
) {
    Column(
        modifier =
            Modifier
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(
            text = stringResource(Res.string.review_report_description),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
        ReportReason.entries.forEach { reason ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selectedReason == reason,
                            enabled = !isActing,
                            role = Role.RadioButton,
                        ) { onReasonSelected(reason) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selectedReason == reason,
                    onClick = null,
                    enabled = !isActing,
                )
                AppText(
                    text = stringResource(reportReasonResource(reason)),
                    modifier = Modifier.padding(start = 12.dp),
                    style = AppTextStyle.B3,
                    color = GrayColor.C500,
                )
            }
        }
        OutlinedTextField(
            value = details,
            onValueChange = {
                onDetailsChanged(it.take(ReportReason.MAX_DETAILS_LENGTH))
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                AppText(
                    text = stringResource(Res.string.review_report_details),
                    style = AppTextStyle.B3,
                    color = GrayColor.C500,
                )
            },
            enabled = !isActing,
            minLines = 2,
        )
        if (actionFailed) {
            AppText(
                text = stringResource(Res.string.review_action_failed),
                color = SystemColor.Warning,
                style = AppTextStyle.B3,
            )
        }
    }
}

private fun reportReasonResource(reason: ReportReason): StringResource =
    when (reason) {
        ReportReason.SPAM -> Res.string.review_reason_spam
        ReportReason.HARASSMENT -> Res.string.review_reason_harassment
        ReportReason.HATE -> Res.string.review_reason_hate
        ReportReason.SEXUAL_CONTENT -> Res.string.review_reason_sexual
        ReportReason.VIOLENCE -> Res.string.review_reason_violence
        ReportReason.PRIVACY -> Res.string.review_reason_privacy
        ReportReason.OTHER -> Res.string.review_reason_other
    }
