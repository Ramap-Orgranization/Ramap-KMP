package com.peto.ramap.ui.review.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentBody
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.LocalAppTypography
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import com.peto.ramap.ui.review.detail.contract.ReviewDetailUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_close
import ramap.shared.generated.resources.ic_send_paper_plane
import ramap.shared.generated.resources.ic_signup_check
import ramap.shared.generated.resources.profanity_input_invalid
import ramap.shared.generated.resources.review_close
import ramap.shared.generated.resources.review_comment_edit_save
import ramap.shared.generated.resources.review_comment_edit_title
import ramap.shared.generated.resources.review_comment_mention
import ramap.shared.generated.resources.review_comment_placeholder
import ramap.shared.generated.resources.review_comment_reply_placeholder
import ramap.shared.generated.resources.review_comment_reply_target
import ramap.shared.generated.resources.review_comment_submit
import ramap.shared.generated.resources.review_comment_submit_failed
import ramap.shared.generated.resources.shop_review_cancel

@Composable
internal fun ReviewCommentComposer(
    state: ReviewDetailUiState,
    onIntent: (ReviewDetailIntent) -> Unit,
    focusRequester: FocusRequester,
) {
    val edit = state.commentEdit
    val replyTarget = state.replyTarget.takeIf { edit == null }
    val header =
        if (edit != null) {
            AnnotatedString(stringResource(Res.string.review_comment_edit_title))
        } else {
            replyTarget?.let { replyTargetLabel(it.nickname) }
        }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(edit?.commentId, replyTarget?.id) {
        if (edit != null || replyTarget != null) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val draft = state.composerDraft
    val count = Review.bodyCharacterCount(draft.trim())
    val hasProfanity = Review.containsProfanity(draft)
    val canSubmit =
        count in 1..ReviewCommentBody.MAX_LENGTH &&
            !hasProfanity &&
            state.commentsLoaded &&
            (!state.isChangingComments || state.isSubmitting)
    val submitFailed = edit == null && state.submitFailed
    val panelShape = RoundedCornerShape(22.dp)

    if (header == null) HorizontalDivider(color = GrayColor.C100)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .then(
                    if (header == null) {
                        Modifier
                    } else {
                        Modifier
                            .shadow(3.dp, panelShape)
                            .background(CommonColor.White, panelShape)
                            .border(1.dp, GrayColor.C100, panelShape)
                    },
                ).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (header != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = header,
                    modifier = Modifier.weight(1f),
                    style = AppTextStyle.B2,
                    color = GrayColor.C400,
                )
                AppIconButton(
                    icon = Res.drawable.ic_close,
                    contentDescription = stringResource(if (edit == null) Res.string.review_close else Res.string.shop_review_cancel),
                    onClick = { onIntent(if (edit == null) ReviewDetailIntent.CancelReply else ReviewDetailIntent.CancelCommentEdit) },
                    modifier = Modifier.size(36.dp),
                    enabled = !state.isChangingComments,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { onIntent(ReviewDetailIntent.ChangeCommentDraft(text = it, editingCommentId = edit?.commentId)) },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                enabled = !state.isChangingComments,
                textStyle = LocalAppTypography.current.b2,
                placeholder = {
                    AppText(
                        text = stringResource(if (replyTarget == null) Res.string.review_comment_placeholder else Res.string.review_comment_reply_placeholder),
                        style = AppTextStyle.B2,
                        color = GrayColor.C400,
                    )
                },
                isError = hasProfanity || count > ReviewCommentBody.MAX_LENGTH,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GrayColor.C050,
                        unfocusedContainerColor = GrayColor.C050,
                        disabledContainerColor = GrayColor.C050,
                        errorContainerColor = GrayColor.C050,
                        focusedBorderColor = GrayColor.C200,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = SystemColor.Warning,
                        focusedTextColor = GrayColor.C500,
                        unfocusedTextColor = GrayColor.C500,
                        cursorColor = GrayColor.C500,
                    ),
            )
            AppIconButton(
                icon = if (edit == null) Res.drawable.ic_send_paper_plane else Res.drawable.ic_signup_check,
                contentDescription = stringResource(if (edit == null) Res.string.review_comment_submit else Res.string.review_comment_edit_save),
                onClick = {
                    onIntent(if (edit == null) ReviewDetailIntent.Submit else ReviewDetailIntent.SaveCommentEdit(edit.commentId))
                },
                modifier =
                    Modifier
                        .padding(bottom = 5.dp)
                        .size(44.dp)
                        .background(
                            color = if (canSubmit) ChromaticColor.Orange400 else ChromaticColor.Orange050,
                            shape = CircleShape,
                        ),
                tint = CommonColor.White,
                enabled = canSubmit,
                isLoading = state.isSubmitting,
            )
        }
        if (hasProfanity || submitFailed) {
            AppText(
                text = stringResource(if (hasProfanity) Res.string.profanity_input_invalid else Res.string.review_comment_submit_failed),
                modifier = Modifier.padding(horizontal = 8.dp),
                style = AppTextStyle.C1,
                color = SystemColor.Warning,
            )
        }
    }
}

@Composable
private fun replyTargetLabel(nickname: String): AnnotatedString {
    val mention = stringResource(Res.string.review_comment_mention, nickname)
    val label = stringResource(Res.string.review_comment_reply_target, mention)
    return buildAnnotatedString {
        append(label)
        val mentionStart = label.indexOf(mention)
        if (mentionStart >= 0) {
            addStyle(
                style = SpanStyle(color = ChromaticColor.Blue500),
                start = mentionStart,
                end = mentionStart + mention.length,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCommentComposerPreview() {
    RamapTheme {
        ReviewCommentComposer(
            state =
                ReviewDetailUiState(
                    draft = "정말 맛있어 보이네요!",
                    commentsLoaded = true,
                ),
            onIntent = {},
            focusRequester = remember { FocusRequester() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCommentComposerReplyPreview() {
    RamapTheme {
        ReviewCommentComposer(
            state =
                ReviewDetailUiState(
                    replyTarget =
                        ReviewComment(
                            id = "comment-1",
                            parentId = null,
                            body = "추천 라멘집 알려주세요!",
                            nickname = "라멘러버",
                            createdAt = "2026-02-15T13:00:00Z",
                        ),
                    commentsLoaded = true,
                ),
            onIntent = {},
            focusRequester = remember { FocusRequester() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewCommentComposerSubmitFailedPreview() {
    RamapTheme {
        ReviewCommentComposer(
            state =
                ReviewDetailUiState(
                    draft = "작성에 실패한 댓글 내용입니다.",
                    submitFailed = true,
                    commentsLoaded = true,
                ),
            onIntent = {},
            focusRequester = remember { FocusRequester() },
        )
    }
}
