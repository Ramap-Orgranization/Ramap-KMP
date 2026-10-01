package com.peto.ramap.designsystem.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_report_warning_circle
import ramap.shared.generated.resources.review_close
import ramap.shared.generated.resources.review_report_false_warning
import ramap.shared.generated.resources.review_report_send
import ramap.shared.generated.resources.review_report_title
import ramap.shared.generated.resources.shop_review_cancel

@Composable
fun ReviewReportDialog(
    visible: Boolean,
    selectedReason: ReportReason?,
    onReasonSelected: (ReportReason) -> Unit,
    details: String,
    onDetailsChanged: (String) -> Unit,
    isActing: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (ReportReason, String) -> Unit,
) {
    if (!visible) return

    Dialog(
        onDismissRequest = { if (!isActing) onDismissRequest() },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !isActing,
                dismissOnClickOutside = !isActing,
            ),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            ReviewReportDialogContent(
                selectedReason = selectedReason,
                onReasonSelected = onReasonSelected,
                details = details,
                onDetailsChanged = onDetailsChanged,
                isActing = isActing,
                onDismissRequest = onDismissRequest,
                onSubmit = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReviewReportDialogContent(
    selectedReason: ReportReason?,
    onReasonSelected: (ReportReason) -> Unit,
    details: String,
    onDetailsChanged: (String) -> Unit,
    isActing: Boolean,
    onDismissRequest: () -> Unit,
    onSubmit: (ReportReason, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CommonColor.White,
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    text = stringResource(Res.string.review_report_title),
                    style = AppTextStyle.T1,
                    color = GrayColor.C500,
                )
                IconButton(
                    onClick = onDismissRequest,
                    enabled = !isActing,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.review_close),
                        tint = GrayColor.C400,
                    )
                }
            }
            ReviewReportFormContent(
                selectedReason = selectedReason,
                onReasonSelected = onReasonSelected,
                details = details,
                onDetailsChanged = onDetailsChanged,
                isActing = isActing,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
            )
            HorizontalDivider(color = GrayColor.C100)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_report_warning_circle),
                    contentDescription = null,
                    tint = SystemColor.Warning,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                AppText(
                    text = stringResource(Res.string.review_report_false_warning),
                    style = AppTextStyle.C1,
                    color = SystemColor.Warning,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppButton(
                    text = stringResource(Res.string.shop_review_cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                    enabled = !isActing,
                    textStyle = AppTextStyle.B1,
                    textColor = GrayColor.C300,
                    backgroundColor = GrayColor.C050,
                    disabledBackgroundColor = GrayColor.C050,
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppButton(
                    text = stringResource(Res.string.review_report_send),
                    onClick = {
                        selectedReason?.takeIf { it.isValidDetails(details) }?.let { onSubmit(it, details) }
                    },
                    modifier = Modifier.weight(1.6f),
                    enabled = selectedReason?.isValidDetails(details) == true && !isActing,
                    isLoading = isActing,
                    textStyle = AppTextStyle.B1,
                    backgroundColor = GrayColor.C500,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 324, heightDp = 641)
@Composable
private fun ReviewReportDialogContentPreview() {
    RamapTheme {
        ReviewReportDialogContent(
            selectedReason = ReportReason.SPAM,
            onReasonSelected = {},
            details = "같은 광고 문구가 반복됩니다.",
            onDetailsChanged = {},
            isActing = false,
            onDismissRequest = {},
            onSubmit = { _, _ -> },
            modifier = Modifier.fillMaxWidth().height(641.dp),
        )
    }
}
