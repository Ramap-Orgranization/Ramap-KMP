package com.peto.ramap.ui.profile.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.button.AppButton
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.review.ReviewCard
import com.peto.ramap.designsystem.review.ReviewPage
import com.peto.ramap.designsystem.review.ReviewPixelHeader
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.community.ReportReason
import com.peto.ramap.domain.model.community.ReviewModerationStatus
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.theme.SystemColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.profile.review.component.FeedEmpty
import com.peto.ramap.ui.profile.review.component.FeedLoading
import com.peto.ramap.ui.profile.review.component.ProfileHeader
import com.peto.ramap.ui.profile.review.contract.ProfileReviewIntent
import com.peto.ramap.ui.profile.review.contract.ProfileReviewSideEffect
import com.peto.ramap.ui.profile.review.contract.ProfileReviewTarget
import com.peto.ramap.ui.profile.review.contract.ProfileReviewUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.community_guidelines_title
import ramap.shared.generated.resources.review_action_failed
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_action_success_description
import ramap.shared.generated.resources.review_block
import ramap.shared.generated.resources.review_block_confirm
import ramap.shared.generated.resources.review_close
import ramap.shared.generated.resources.review_delete
import ramap.shared.generated.resources.review_delete_confirm
import ramap.shared.generated.resources.review_delete_confirm_title
import ramap.shared.generated.resources.review_delete_failed
import ramap.shared.generated.resources.review_load_failed
import ramap.shared.generated.resources.review_profile_title
import ramap.shared.generated.resources.review_report
import ramap.shared.generated.resources.review_report_send
import ramap.shared.generated.resources.review_report_user
import ramap.shared.generated.resources.review_retry
import ramap.shared.generated.resources.review_unblock
import ramap.shared.generated.resources.review_unblock_confirm
import ramap.shared.generated.resources.shop_review_cancel
import ramap.shared.generated.resources.shop_review_load_more

@Composable
fun ReviewProfileRoute(
    userId: String,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onGuidelines: () -> Unit,
    onShopClick: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    viewModel: ProfileReviewViewModel = koinViewModel(),
) {
    ReviewFeedRoute(
        target = ProfileReviewTarget(userId),
        onBack = onBack,
        onLogin = onLogin,
        onOpenProfile = onOpenProfile,
        onGuidelines = onGuidelines,
        onShopClick = onShopClick,
        onEditReview = onEditReview,
        viewModel = viewModel,
    )
}

@Composable
private fun ReviewFeedRoute(
    target: ProfileReviewTarget,
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onGuidelines: () -> Unit,
    onShopClick: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    viewModel: ProfileReviewViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(target) {
        viewModel.dispatch(ProfileReviewIntent.OpenTarget(target))
    }
    ObserveAsEvents(viewModel.sideEffect) {
        when (it) {
            ProfileReviewSideEffect.LoginRequired -> onLogin()
        }
    }

    ReviewPage(
        title = stringResource(Res.string.review_profile_title),
        onBack = onBack,
        action = {
            AppButton(
                text = stringResource(Res.string.community_guidelines_title),
                onClick = onGuidelines,
                textStyle = AppTextStyle.B3,
                textColor = GrayColor.C500,
                backgroundColor = CommonColor.White,
                cornerRadius = 0.dp,
            )
        },
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                if (state.profile != null) {
                    ProfileHeader(
                        state = state,
                        onReport = { viewModel.dispatch(ProfileReviewIntent.ReportProfile) },
                        onBlock = { viewModel.dispatch(ProfileReviewIntent.ConfirmBlock(it)) },
                    )
                } else {
                    ReviewPixelHeader(
                        title = stringResource(Res.string.review_profile_title),
                    )
                }
            }
            if (state.isLoading && state.reviews.isEmpty()) {
                item {
                    FeedLoading()
                }
            }
            if (state.loadFailed) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppText(
                            text = stringResource(Res.string.review_load_failed),
                            color = SystemColor.Warning,
                            style = AppTextStyle.B3,
                        )
                        AppButton(
                            text = stringResource(Res.string.review_retry),
                            onClick = {
                                viewModel.dispatch(
                                    if (state.reviews.isEmpty()) {
                                        ProfileReviewIntent.Load
                                    } else {
                                        ProfileReviewIntent.LoadMore
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 0.dp,
                        )
                    }
                }
            }
            if (!state.isLoading && !state.loadFailed && state.reviews.isEmpty()) {
                item {
                    FeedEmpty(
                        state = state,
                    )
                }
            }
            items(state.reviews, key = { it.id }) { review ->
                ReviewCard(
                    review = review,
                    onOpenProfile = onOpenProfile,
                    onReport =
                        if (review.author.userId.isNotBlank() && review.author.userId != state.currentUserId) {
                            { viewModel.dispatch(ProfileReviewIntent.ReportReview(review)) }
                        } else {
                            null
                        },
                    onShopClick = { onShopClick(review.shopId) },
                    onLike =
                        if (review.isPublic && review.moderationStatus == ReviewModerationStatus.PUBLISHED && review.author.userId != state.currentUserId) {
                            { viewModel.dispatch(ProfileReviewIntent.ToggleLike(review)) }
                        } else {
                            null
                        },
                    isLikeLoading = state.likingReviewId == review.id,
                    actionsEnabled = !state.isLiking && !state.isDeleting && !state.isActing,
                    onEdit =
                        if (review.author.userId == state.currentUserId) {
                            { onEditReview(review.shopId, review.id) }
                        } else {
                            null
                        },
                    onDelete =
                        if (review.author.userId == state.currentUserId) {
                            { viewModel.dispatch(ProfileReviewIntent.ConfirmDelete(review)) }
                        } else {
                            null
                        },
                )
            }
            if (state.hasMore && !state.isBlocked && state.reviews.isNotEmpty()) {
                item {
                    AppButton(
                        text = stringResource(Res.string.shop_review_load_more),
                        onClick = { viewModel.dispatch(ProfileReviewIntent.LoadMore) },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = state.isLoading,
                        cornerRadius = 0.dp,
                    )
                }
            }
        }
    }
    var selectedReason by remember(state.reportTargetId) { mutableStateOf<ReportReason?>(null) }
    var reportDetails by remember(state.reportTargetId) { mutableStateOf("") }
    val dismissAction = { viewModel.dispatch(ProfileReviewIntent.DismissAction) }

    CommonDialog(
        visible = state.reportTargetId != null,
        confirmText = stringResource(Res.string.review_report_send),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = selectedReason != null && !state.isActing,
        confirmIsLoading = state.isActing,
        dismissEnabled = !state.isActing,
        dismissOnBackPress = !state.isActing,
        dismissOnClickOutside = !state.isActing,
        onDismissRequest = { if (!state.isActing) dismissAction() },
        onDismiss = dismissAction,
        onConfirm = {
            selectedReason?.let { reason ->
                viewModel.dispatch(ProfileReviewIntent.SendReport(reason, reportDetails))
            }
        },
    ) {
        AppText(
            text =
                stringResource(
                    if (state.reportingUser) {
                        Res.string.review_report_user
                    } else {
                        Res.string.review_report
                    },
                ),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
        ReviewReportFormContent(
            selectedReason = selectedReason,
            onReasonSelected = { selectedReason = it },
            details = reportDetails,
            onDetailsChanged = { reportDetails = it },
            isActing = state.isActing,
            actionFailed = state.actionFailed,
        )
    }

    CommonDialog(
        visible = state.deleteTarget != null,
        confirmText = stringResource(Res.string.review_delete),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !state.isDeleting,
        confirmIsLoading = state.isDeleting,
        dismissEnabled = !state.isDeleting,
        onDismissRequest = { if (!state.isDeleting) dismissAction() },
        onDismiss = dismissAction,
        onConfirm = { viewModel.dispatch(ProfileReviewIntent.DeleteReview) },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(
                text = stringResource(Res.string.review_delete_confirm_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
            )

            AppText(
                text = stringResource(Res.string.review_delete_confirm),
                style = AppTextStyle.B3,
                color = GrayColor.C500,
            )
        }
        if (state.actionFailed) {
            AppText(
                text = stringResource(Res.string.review_delete_failed),
                style = AppTextStyle.B3,
                color = SystemColor.Warning,
            )
        }
    }

    val blockTarget = state.blockTarget
    val unblock = blockTarget != null && state.blockedUsers.any { it.userId == blockTarget.userId }
    CommonDialog(
        visible = blockTarget != null,
        confirmText =
            stringResource(if (unblock) Res.string.review_unblock else Res.string.review_block),
        dismissText = stringResource(Res.string.shop_review_cancel),
        confirmEnabled = !state.isActing,
        confirmIsLoading = state.isActing,
        dismissEnabled = !state.isActing,
        dismissOnBackPress = !state.isActing,
        dismissOnClickOutside = !state.isActing,
        onDismissRequest = { if (!state.isActing) dismissAction() },
        onDismiss = dismissAction,
        onConfirm = { viewModel.dispatch(ProfileReviewIntent.ToggleBlock) },
    ) {
        AppText(
            text =
                stringResource(if (unblock) Res.string.review_unblock else Res.string.review_block),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
        if (blockTarget != null) {
            AppText(
                text =
                    stringResource(
                        if (unblock) {
                            Res.string.review_unblock_confirm
                        } else {
                            Res.string.review_block_confirm
                        },
                        blockTarget.nickname,
                    ),
                style = AppTextStyle.B3,
                color = GrayColor.C500,
            )
        }
        if (state.actionFailed) {
            AppText(
                text = stringResource(Res.string.review_action_failed),
                style = AppTextStyle.B3,
                color = SystemColor.Warning,
            )
        }
    }

    CommonDialog(
        visible = state.actionSucceeded,
        confirmText = stringResource(Res.string.review_close),
        onDismissRequest = dismissAction,
        onConfirm = dismissAction,
    ) {
        AppText(
            text = stringResource(Res.string.review_action_success),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
        AppText(
            text = stringResource(Res.string.review_action_success_description),
            style = AppTextStyle.B3,
            color = GrayColor.C500,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewProfileRoutePreview() {
    RamapTheme {
        ReviewPage(
            title = "리뷰 프로필",
            onBack = {},
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item {
                    ProfileHeader(
                        state =
                            ProfileReviewUiState(
                                profile =
                                    PublicProfile(
                                        userId = "user-1",
                                        nickname = "면발수집가",
                                        bio = "라멘 탐험가입니다.",
                                        avatarUrl = null,
                                    ),
                            ),
                        onReport = {},
                        onBlock = {},
                    )
                }
            }
        }
    }
}
