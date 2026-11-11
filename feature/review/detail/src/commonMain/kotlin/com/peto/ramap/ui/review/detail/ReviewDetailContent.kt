package com.peto.ramap.ui.review.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.review.ReviewCardHeader
import com.peto.ramap.designsystem.review.ReviewCardOverflowMenu
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.review.reviewWrittenDateTimeLabel
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.preview.ReviewDetailPreviewParameterProvider
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.review.detail.component.ReviewCommentComposer
import com.peto.ramap.ui.review.detail.component.ReviewCommentStatus
import com.peto.ramap.ui.review.detail.component.ReviewCommentThreadContent
import com.peto.ramap.ui.review.detail.component.ReviewCommentsFooter
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import com.peto.ramap.ui.review.detail.contract.ReviewDetailLoadKey
import com.peto.ramap.ui.review.detail.contract.ReviewDetailUiState
import org.jetbrains.compose.resources.stringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.map_shop_detail_error_description
import ramap.shared.generated.resources.review_comments
import ramap.shared.generated.resources.review_comments_total
import ramap.shared.generated.resources.review_detail_load_error_3d
import ramap.shared.generated.resources.review_detail_title
import ramap.shared.generated.resources.review_detail_unavailable
import ramap.shared.generated.resources.shop_review_load_failure_message

@Composable
internal fun ReviewDetailContent(
    state: ReviewDetailUiState,
    actions: ReviewCardActions,
    onBack: () -> Unit,
    onOpenShop: (String) -> Unit,
    onLogin: () -> Unit,
    onReportComment: (ReviewComment) -> Unit,
    onDeleteComment: (ReviewComment) -> Unit,
    onIntent: (ReviewDetailIntent) -> Unit,
) {
    val reviewActionsEnabled = !state.loadState.isLoading(ReviewDetailLoadKey.Delete) && !state.isRefreshingSession
    ReviewPage(
        title = stringResource(Res.string.review_detail_title),
        onBack = onBack,
        modifier = Modifier.background(CommonColor.White),
        action = {
            state.detail?.let { detail ->
                ReviewCardOverflowMenu(
                    review = detail.review,
                    actions = actions,
                    actionsEnabled = reviewActionsEnabled,
                    modifier = Modifier.padding(end = 18.dp),
                    notificationsEnabled = detail.reviewNotificationsEnabled,
                    isNotificationUpdating = state.loadState.isLoading(ReviewDetailLoadKey.Notifications),
                )
            }
        },
    ) {
        val detail = state.detail
        when {
            detail != null ->
                Column(modifier = Modifier.fillMaxSize().imePadding()) {
                    val commentFocusRequester = remember { FocusRequester() }
                    val keyboard = LocalSoftwareKeyboardController.current
                    val canWriteComments = detail.canWriteComments && !state.isRefreshingSession
                    val onWriteComment: (() -> Unit)? =
                        when {
                            canWriteComments -> {
                                {
                                    commentFocusRequester.requestFocus()
                                    keyboard?.show()
                                }
                            }
                            state.viewerId == null || state.isRefreshingSession -> onLogin
                            else -> null
                        }
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        item {
                            ReviewCard(
                                review = detail.review,
                                actions = actions,
                                currentUserId = state.viewerId,
                                isLikeLoading = state.loadState.isLoading(ReviewDetailLoadKey.Like),
                                actionsEnabled = reviewActionsEnabled,
                                showFullBody = true,
                                showWrittenTime = false,
                                photoViewerEnabled = true,
                                header = {
                                    ReviewCardHeader(
                                        review = detail.review,
                                        actions = ReviewCardActions(onOpenProfile = actions.onOpenProfile),
                                        actionsEnabled = reviewActionsEnabled,
                                        shop = detail.shop,
                                        onOpenShop = { onOpenShop(detail.review.shopId) },
                                        fallbackShopName = detail.shopName,
                                        authorSubtitle = {
                                            reviewWrittenDateTimeLabel(detail.review.createdAt)?.let { writtenAt ->
                                                AppText(
                                                    text = writtenAt,
                                                    style = AppTextStyle.B2,
                                                    color = GrayColor.C300,
                                                )
                                            }
                                        },
                                    )
                                },
                            )
                        }
                        item {
                            val count = detail.review.commentCount
                            AppText(
                                text = count?.let { stringResource(Res.string.review_comments_total, it) } ?: stringResource(Res.string.review_comments),
                                style = AppTextStyle.T2,
                                color = GrayColor.C500,
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
                            )
                        }
                        items(items = state.threads, key = { it.comment.id }) { thread ->
                            ReviewCommentThreadContent(
                                thread = thread,
                                state = state,
                                onIntent = onIntent,
                                showReplyAction = canWriteComments && !state.isChangingComments,
                                onReportComment = onReportComment.takeUnless { state.isRefreshingSession },
                                onEditComment =
                                    if (canWriteComments && !state.isChangingComments) {
                                        { comment -> onIntent(ReviewDetailIntent.StartCommentEdit(comment.id)) }
                                    } else {
                                        null
                                    },
                                onDeleteComment = onDeleteComment.takeUnless { state.isChangingComments || state.isRefreshingSession },
                            )
                        }
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ReviewCommentsFooter(
                                    state = state,
                                    onIntent = onIntent,
                                    onWriteComment = onWriteComment,
                                )
                            }
                        }
                    }
                    if (canWriteComments) {
                        ReviewCommentComposer(
                            state = state,
                            onIntent = onIntent,
                            focusRequester = commentFocusRequester,
                        )
                    }
                }
            state.unavailable ->
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReviewCommentStatus(stringResource(Res.string.review_detail_unavailable))
                }
            state.detailFailed ->
                LoadErrorContent(
                    image = Res.drawable.review_detail_load_error_3d,
                    title = stringResource(Res.string.shop_review_load_failure_message),
                    description = stringResource(Res.string.map_shop_detail_error_description),
                    onRetry = { onIntent(ReviewDetailIntent.RetryDetail) },
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                )
            else -> RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewDetailContentPreview(
    @PreviewParameter(ReviewDetailPreviewParameterProvider::class) detail: ReviewDetail,
) {
    val state =
        ReviewDetailUiState(
            viewerId = "user-1",
            detail = detail,
            threads = ReviewDetailPreviewParameterProvider().sampleThreads,
            commentsLoaded = true,
        )
    RamapTheme {
        ReviewDetailContent(
            state = state,
            actions = ReviewCardActions(),
            onBack = {},
            onOpenShop = {},
            onLogin = {},
            onReportComment = {},
            onDeleteComment = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewDetailContentSubmitFailedPreview(
    @PreviewParameter(ReviewDetailPreviewParameterProvider::class) detail: ReviewDetail,
) {
    val state =
        ReviewDetailUiState(
            viewerId = "user-1",
            detail = detail,
            threads = ReviewDetailPreviewParameterProvider().sampleThreads,
            commentsLoaded = true,
            draft = "작성에 실패한 댓글 내용입니다.",
            submitFailed = true,
        )
    RamapTheme {
        ReviewDetailContent(
            state = state,
            actions = ReviewCardActions(),
            onBack = {},
            onOpenShop = {},
            onLogin = {},
            onReportComment = {},
            onDeleteComment = {},
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewDetailContentDetailFailedPreview() {
    val state = ReviewDetailUiState(detailFailed = true)
    RamapTheme {
        ReviewDetailContent(
            state = state,
            actions = ReviewCardActions(),
            onBack = {},
            onOpenShop = {},
            onLogin = {},
            onReportComment = {},
            onDeleteComment = {},
            onIntent = {},
        )
    }
}
