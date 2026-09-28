package com.peto.ramap.ui.review.write.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_body_count
import ramap.shared.generated.resources.review_body_required
import ramap.shared.generated.resources.review_body_title
import ramap.shared.generated.resources.shop_review_placeholder

@Composable
internal fun ReviewTextEditor(
    state: ReviewWriteUiState,
    onIntent: (ReviewWriteIntent) -> Unit,
) {
    val bodyCharacterCount = Review.bodyCharacterCount(state.body)
    val isValidBody = Review.isValidBody(state.body)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(text = stringResource(Res.string.review_body_title), style = AppTextStyle.T2, color = GrayColor.C500)
            AppText(text = stringResource(Res.string.review_body_required), style = AppTextStyle.C1, color = SystemColor.Warning)
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GrayColor.C050,
        ) {
            Column(
                modifier = Modifier.padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.body,
                    onValueChange = { onIntent(ReviewWriteIntent.ChangeBody(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                    placeholder = {
                        AppText(
                            text = stringResource(Res.string.shop_review_placeholder),
                            style = AppTextStyle.B2,
                            color = GrayColor.C400,
                        )
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            AppText(
                                text = stringResource(Res.string.review_body_count, bodyCharacterCount, Review.BODY_LENGTH.last),
                                style = AppTextStyle.C1,
                                color = if (isValidBody) GrayColor.C500 else GrayColor.C400,
                            )
                        }
                    },
                    minLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    isError = state.body.isNotBlank() && !isValidBody,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                        ),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Valid")
@Composable
private fun ReviewTextEditorValidPreview() {
    RamapTheme {
        ReviewTextEditor(
            state = ReviewWriteUiState(body = "국물이 진하고 면발이 쫄깃해요"),
            onIntent = {},
        )
    }
}
