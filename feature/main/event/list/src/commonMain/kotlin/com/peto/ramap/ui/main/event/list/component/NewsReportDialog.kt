package com.peto.ramap.ui.main.event.list.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheet
import com.peto.ramap.designsystem.bottomsheet.CommonBottomSheetConfig
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.report.NewsReportEvidence
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_camera_add
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_info
import ramap.shared.generated.resources.ic_link
import ramap.shared.generated.resources.ic_photo_library
import ramap.shared.generated.resources.news_report_action
import ramap.shared.generated.resources.news_report_clear_action
import ramap.shared.generated.resources.news_report_description
import ramap.shared.generated.resources.news_report_dismiss
import ramap.shared.generated.resources.news_report_link_label
import ramap.shared.generated.resources.news_report_photo_add
import ramap.shared.generated.resources.news_report_photo_label
import ramap.shared.generated.resources.news_report_photo_remove
import ramap.shared.generated.resources.news_report_placeholder
import ramap.shared.generated.resources.news_report_title

@Composable
internal fun NewsReportDialog(
    value: String,
    evidence: NewsReportEvidence?,
    visible: Boolean,
    isSubmitting: Boolean,
    onValueChange: (String) -> Unit,
    onImagePick: () -> Unit,
    onEvidenceRemove: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val clearActionDescription = stringResource(Res.string.news_report_clear_action)
    val photoAddDescription = stringResource(Res.string.news_report_photo_add)

    CommonBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        config =
            CommonBottomSheetConfig(
                isDraggable = true,
                maxHeightFraction = 0.9f,
            ),
    ) { draggableModifier ->
        Column(
            modifier =
                draggableModifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 5.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AppText(
                text = stringResource(Res.string.news_report_title),
                style = AppTextStyle.H4,
                color = GrayColor.C500,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(GrayColor.C050, RoundedCornerShape(16.dp))
                        .padding(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                    tint = GrayColor.C400,
                )
                AppText(
                    text = stringResource(Res.string.news_report_description),
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(
                    icon = Res.drawable.ic_link,
                    text = stringResource(Res.string.news_report_link_label),
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        AppText(
                            text = stringResource(Res.string.news_report_placeholder),
                            style = AppTextStyle.B2,
                            color = GrayColor.C300,
                        )
                    },
                    maxLines = 1,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                onSubmit()
                                focusManager.clearFocus()
                            },
                        ),
                    trailingIcon = {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = { onValueChange("") }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = clearActionDescription,
                                    tint = GrayColor.C400,
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CommonColor.White,
                            unfocusedContainerColor = CommonColor.White,
                            focusedBorderColor = GrayColor.C300,
                            unfocusedBorderColor = GrayColor.C200,
                        ),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(
                    icon = Res.drawable.ic_photo_library,
                    text = stringResource(Res.string.news_report_photo_label),
                )

                if (evidence == null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 5f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GrayColor.C050)
                                .drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    drawRoundRect(
                                        color = GrayColor.C200,
                                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                        size =
                                            Size(
                                                size.width - strokeWidth,
                                                size.height - strokeWidth,
                                            ),
                                        cornerRadius = CornerRadius(16.dp.toPx()),
                                        style =
                                            Stroke(
                                                width = strokeWidth,
                                                pathEffect =
                                                    PathEffect.dashPathEffect(
                                                        intervals =
                                                            floatArrayOf(
                                                                8.dp.toPx(),
                                                                4.dp.toPx(),
                                                            ),
                                                    ),
                                            ),
                                    )
                                }.semantics { contentDescription = photoAddDescription }
                                .clickable(role = Role.Button, onClick = onImagePick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera_add),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = GrayColor.C400,
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 5f)
                                .clip(RoundedCornerShape(16.dp)),
                    ) {
                        AsyncImage(
                            model = evidence.bytes,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = onEvidenceRemove,
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(20.dp)
                                    .size(20.dp)
                                    .border(
                                        width = 1.dp,
                                        color = GrayColor.C100,
                                        shape = CircleShape,
                                    ).background(CommonColor.White, CircleShape),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = stringResource(Res.string.news_report_photo_remove),
                                tint = GrayColor.C500,
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.news_report_dismiss),
                    textColor = GrayColor.C500,
                    backgroundColor = CommonColor.White,
                    border = BorderStroke(1.dp, GrayColor.C200),
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppButton(
                    modifier = Modifier.weight(1.6f),
                    text = stringResource(Res.string.news_report_action),
                    enabled = (value.isNotBlank() || evidence != null) && !isSubmitting,
                    isLoading = isSubmitting,
                    backgroundColor = CommonColor.Black,
                    onClick = onSubmit,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    icon: DrawableResource,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = GrayColor.C500,
        )
        AppText(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = AppTextStyle.T3,
            color = GrayColor.C500,
        )
    }
}
