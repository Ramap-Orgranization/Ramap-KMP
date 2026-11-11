package com.peto.ramap.ui.review.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.button.AppIconButton
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.menu.AppDropdownMenu
import com.peto.ramap.designsystem.menu.AppDropdownMenuItem
import com.peto.ramap.designsystem.profile.ProfileAvatar
import com.peto.ramap.designsystem.review.ReviewWriteCard
import com.peto.ramap.designsystem.review.reviewWrittenTimeLabel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.ChromaticColor
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import com.peto.ramap.ui.review.detail.contract.ReviewDetailLoadKey
import com.peto.ramap.ui.review.detail.contract.ReviewDetailUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ic_more_vert
import ramap.shared.generated.resources.ic_report_flag
import ramap.shared.generated.resources.ic_review_comment_outline
import ramap.shared.generated.resources.review_comment_actions
import ramap.shared.generated.resources.review_comment_deleted
import ramap.shared.generated.resources.review_comment_edited
import ramap.shared.generated.resources.review_comment_empty_illustration
import ramap.shared.generated.resources.review_comment_mention
import ramap.shared.generated.resources.review_comment_prompt_title
import ramap.shared.generated.resources.review_comment_replies_more
import ramap.shared.generated.resources.review_comment_reply
import ramap.shared.generated.resources.review_comment_report_title
import ramap.shared.generated.resources.review_comment_write_action
import ramap.shared.generated.resources.review_comments_empty
import ramap.shared.generated.resources.review_comments_load_failed
import ramap.shared.generated.resources.review_comments_more
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_detail_unavailable
import ramap.shared.generated.resources.review_edit
import ramap.shared.generated.resources.review_retry

@Composable
internal fun ReviewCommentThreadContent(
    thread: ReviewCommentThread,
    state: ReviewDetailUiState,
    onIntent: (ReviewDetailIntent) -> Unit,
    showReplyAction: Boolean,
    onReportComment: ((ReviewComment) -> Unit)? = null,
    onEditComment: ((ReviewComment) -> Unit)? = null,
    onDeleteComment: ((ReviewComment) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ReviewCommentItem(
            comment = thread.comment,
            onReply = if (showReplyAction && !thread.comment.isDeleted) ({ onIntent(ReviewDetailIntent.ReplyTo(thread.comment)) }) else null,
            onReportComment = onReportComment,
            onEditComment = onEditComment,
            onDeleteComment = onDeleteComment,
        )
        thread.replies.forEach { reply ->
            ReviewCommentItem(
                comment = reply,
                replyTo = thread.comment.nickname.takeUnless { thread.comment.isDeleted },
                onReportComment = onReportComment,
                onEditComment = onEditComment,
                onDeleteComment = onDeleteComment,
            )
        }
        if (thread.hasMoreReplies) {
            val parentId = thread.comment.id
            val isLoading = state.loadState.isLoading(ReviewDetailLoadKey.Replies(parentId))
            if (isLoading) {
                RamenLoadingIndicator(modifier = Modifier.height(36.dp))
            } else {
                ReviewDetailTextAction(
                    text = stringResource(if (parentId in state.replyFailures) Res.string.review_retry else Res.string.review_comment_replies_more),
                    onClick = { onIntent(ReviewDetailIntent.LoadMoreReplies(parentId)) },
                    modifier = Modifier.padding(start = 44.dp),
                )
            }
        }
        HorizontalDivider(color = GrayColor.C100)
    }
}

@Composable
internal fun ReviewCommentsFooter(
    state: ReviewDetailUiState,
    onIntent: (ReviewDetailIntent) -> Unit,
    onWriteComment: (() -> Unit)? = null,
) {
    when {
        state.unavailable -> ReviewCommentStatus(stringResource(Res.string.review_detail_unavailable))
        state.loadState.isLoading(ReviewDetailLoadKey.Comments) || (!state.commentsLoaded && !state.commentsFailed) ->
            RamenLoadingIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        state.commentsFailed -> {
            ReviewCommentStatus(stringResource(Res.string.review_comments_load_failed))
            ReviewDetailTextAction(
                text = stringResource(Res.string.review_retry),
                onClick = {
                    onIntent(if (state.nextCursor == null) ReviewDetailIntent.RetryComments else ReviewDetailIntent.LoadMoreComments)
                },
            )
        }
        state.nextCursor != null ->
            ReviewDetailTextAction(
                text = stringResource(Res.string.review_comments_more),
                onClick = { onIntent(ReviewDetailIntent.LoadMoreComments) },
            )
        state.threads.isEmpty() ->
            if (onWriteComment == null) {
                ReviewCommentStatus(stringResource(Res.string.review_comments_empty))
            } else {
                LoadErrorContent(
                    image = Res.drawable.review_comment_empty_illustration,
                    modifier = Modifier.fillMaxWidth(),
                    compact = true,
                )
                ReviewWriteCard(
                    onClick = onWriteComment,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                    prompt = Res.string.review_comment_prompt_title,
                    actionLabel = Res.string.review_comment_write_action,
                    icon = Res.drawable.ic_review_comment_outline,
                    enabled = !state.isChangingComments && !state.isRefreshingSession,
                )
            }
    }
}

@Composable
internal fun ReviewCommentStatus(text: String) {
    AppText(
        text = text,
        modifier = Modifier.padding(vertical = 12.dp),
        style = AppTextStyle.B2,
        color = GrayColor.C400,
    )
}

@Composable
private fun ReviewCommentItem(
    comment: ReviewComment,
    onReportComment: ((ReviewComment) -> Unit)?,
    onEditComment: ((ReviewComment) -> Unit)?,
    onDeleteComment: ((ReviewComment) -> Unit)?,
    replyTo: String? = null,
    onReply: (() -> Unit)? = null,
) {
    if (comment.isDeleted) {
        AppText(
            text = stringResource(Res.string.review_comment_deleted),
            style = AppTextStyle.B2,
            color = GrayColor.C300,
            modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        )
        return
    }

    val isReply = comment.parentId != null
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (isReply) ChromaticColor.Cream400 else CommonColor.White)
                .padding(
                    start = if (isReply) 44.dp else 16.dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
        verticalAlignment = Alignment.Top,
    ) {
        ProfileAvatar(
            model = null,
            description = "",
            modifier = Modifier.size(30.dp).clearAndSetSemantics {},
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = comment.nickname,
                    style = AppTextStyle.T2,
                    color = GrayColor.C500,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                reviewWrittenTimeLabel(comment.createdAt)?.let { time ->
                    AppText(text = time, style = AppTextStyle.C1, color = GrayColor.C300, maxLines = 1)
                }
                if (comment.isEdited) {
                    AppText(
                        text = stringResource(Res.string.review_comment_edited),
                        style = AppTextStyle.C1,
                        color = GrayColor.C300,
                        maxLines = 1,
                    )
                }
            }
            ReviewCommentBodyText(body = comment.body, replyTo = replyTo)
            onReply?.let { CommentRowAction(stringResource(Res.string.review_comment_reply), it) }
        }
        if (comment.canManage && (onEditComment != null || onDeleteComment != null)) {
            ReviewCommentOverflowMenu(
                commentId = comment.id,
                onEdit = onEditComment?.let { edit -> { edit(comment) } },
                onDelete = onDeleteComment?.let { delete -> { delete(comment) } },
            )
        } else if (comment.canReport && onReportComment != null) {
            AppIconButton(
                icon = Res.drawable.ic_report_flag,
                contentDescription = stringResource(Res.string.review_comment_report_title),
                onClick = { onReportComment(comment) },
                modifier = Modifier.size(36.dp),
                tint = GrayColor.C300,
            )
        }
    }
}

@Composable
private fun ReviewCommentOverflowMenu(
    commentId: String,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var expanded by remember(commentId) { mutableStateOf(false) }
    Box {
        AppIconButton(
            icon = Res.drawable.ic_more_vert,
            contentDescription = stringResource(Res.string.review_comment_actions),
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp),
            tint = GrayColor.C300,
        )
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            onEdit?.let { edit ->
                AppDropdownMenuItem(
                    text = stringResource(Res.string.review_edit),
                    onClick = {
                        expanded = false
                        edit()
                    },
                )
            }
            onDelete?.let { delete ->
                AppDropdownMenuItem(
                    text = stringResource(Res.string.review_delete),
                    onClick = {
                        expanded = false
                        delete()
                    },
                )
            }
        }
    }
}

@Composable
private fun CommentRowAction(
    label: String,
    onClick: () -> Unit,
) {
    AppText(
        text = label,
        style = AppTextStyle.C1,
        color = GrayColor.C400,
        modifier =
            Modifier
                .noRippleClickable(onClick = onClick)
                .padding(vertical = 5.dp),
    )
}

@Composable
private fun ReviewCommentBodyText(
    body: String,
    replyTo: String?,
) {
    if (replyTo == null) {
        AppText(text = body, style = AppTextStyle.B2, color = GrayColor.C500)
        return
    }

    val mention = stringResource(Res.string.review_comment_mention, replyTo)
    AppText(
        text =
            buildAnnotatedString {
                withStyle(SpanStyle(color = ChromaticColor.Blue500)) { append(mention) }
                append(' ')
                append(body)
            },
        style = AppTextStyle.B2,
        color = GrayColor.C500,
    )
}
