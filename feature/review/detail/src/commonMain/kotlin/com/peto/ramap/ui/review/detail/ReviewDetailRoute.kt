package com.peto.ramap.ui.review.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.ReviewDeleteDialog
import com.peto.ramap.designsystem.dialog.ReviewReportDialog
import com.peto.ramap.designsystem.review.ReviewCardActions
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.login.rememberLoginGuideRequest
import com.peto.ramap.ui.review.detail.component.ReviewCommentDeleteDialog
import com.peto.ramap.ui.review.detail.contract.ReviewDetailEffect
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import com.peto.ramap.ui.review.detail.contract.ReviewDetailLoadKey
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.login_required_description
import ramap.shared.generated.resources.login_required_review_report_description
import ramap.shared.generated.resources.review_action_success
import ramap.shared.generated.resources.review_comment_report_login_required
import ramap.shared.generated.resources.review_comment_report_policy
import ramap.shared.generated.resources.review_comment_report_title
import ramap.shared.generated.resources.review_report_policy_body
import ramap.shared.generated.resources.review_report_title

@Composable
fun ReviewDetailRoute(
    reviewId: String,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenShop: (String) -> Unit,
    onEditReview: (String, String) -> Unit,
    onLoginTypeSelected: (LoginType) -> Unit,
    onShareReview: (String) -> Unit,
) {
    val viewModel = koinViewModel<ReviewDetailViewModel>(key = reviewId) { parametersOf(reviewId, true) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toastManager = koinInject<ToastManager>()
    val requestLogin = rememberLoginGuideRequest(onLoginTypeSelected = onLoginTypeSelected, ownerKey = viewModel)
    var confirmDelete by remember(state.viewerId) { mutableStateOf(false) }
    var reporting by remember(state.viewerId) { mutableStateOf(false) }
    var reportCommentId by remember(state.viewerId) { mutableStateOf<String?>(null) }
    var reportReason by remember(state.viewerId, reporting) { mutableStateOf<ReportReason?>(null) }
    var reportDetails by remember(state.viewerId, reporting) { mutableStateOf("") }
    var commentToDelete by remember(state.viewerId) { mutableStateOf<ReviewComment?>(null) }
    val isMutatingComment = state.loadState.isLoading(ReviewDetailLoadKey.CommentMutation)

    ObserveAsEvents(viewModel.sideEffect) { effect ->
        when (effect) {
            is ReviewDetailEffect.RequestLogin -> requestLogin(effect.description)
            is ReviewDetailEffect.ShowError -> toastManager.show(ToastData(effect.message, ToastType.ERROR))
            ReviewDetailEffect.Deleted -> onBack()
            ReviewDetailEffect.CommentDeleted -> commentToDelete = null
            ReviewDetailEffect.Reported -> {
                reporting = false
                toastManager.show(ToastData(Res.string.review_action_success, ToastType.SUCCESS))
            }
        }
    }

    val review = state.detail?.review
    val isOwner = review?.author?.userId?.takeIf(String::isNotBlank) == state.viewerId && state.viewerId != null
    val actions =
        ReviewCardActions(
            onOpenProfile = onOpenProfile,
            onLike = if (state.detail?.canLike == true) ({ viewModel.dispatch(ReviewDetailIntent.ToggleLike) }) else null,
            onEdit = if (review != null && isOwner) ({ onEditReview(review.shopId, review.id) }) else null,
            onDelete = if (isOwner) ({ confirmDelete = true }) else null,
            onShare = if (review != null && state.detail?.canShare == true) ({ onShareReview(review.shopId) }) else null,
            onToggleNotifications =
                if (isOwner && state.detail?.reviewNotificationsEnabled != null) {
                    { viewModel.dispatch(ReviewDetailIntent.ToggleNotifications) }
                } else {
                    null
                },
            onReport =
                if (review != null && !isOwner && (review.author.userId.isNotBlank() || review.author.isWithdrawn)) {
                    {
                        if (state.viewerId == null) {
                            requestLogin(Res.string.login_required_review_report_description)
                        } else {
                            reportCommentId = null
                            reporting = true
                        }
                    }
                } else {
                    null
                },
        )
    ReviewDetailContent(
        state = state,
        actions = actions,
        onBack = onBack,
        onOpenShop = onOpenShop,
        onLogin = { requestLogin(Res.string.login_required_description) },
        onReportComment = { comment ->
            if (state.viewerId == null) {
                requestLogin(Res.string.review_comment_report_login_required)
            } else {
                reportCommentId = comment.id
                reporting = true
            }
        },
        onDeleteComment = { commentToDelete = it },
        onIntent = viewModel::dispatch,
    )

    commentToDelete?.takeIf { review != null }?.let { comment ->
        ReviewCommentDeleteDialog(
            isDeleting = isMutatingComment,
            onDismiss = { commentToDelete = null },
            onConfirm = { viewModel.dispatch(ReviewDetailIntent.DeleteComment(comment.id)) },
        )
    }
    ReviewDeleteDialog(
        visible = confirmDelete && isOwner,
        isDeleting = state.loadState.isLoading(ReviewDetailLoadKey.Delete),
        onDismissRequest = { confirmDelete = false },
        onConfirm = {
            confirmDelete = false
            viewModel.dispatch(ReviewDetailIntent.Delete)
        },
    )
    ReviewReportDialog(
        visible = reporting && review != null,
        selectedReason = reportReason,
        onReasonSelected = { reportReason = it },
        details = reportDetails,
        onDetailsChanged = { reportDetails = it },
        isActing = state.loadState.isLoading(ReviewDetailLoadKey.Report),
        onDismissRequest = { reporting = false },
        onSubmit = { reason, details -> viewModel.dispatch(ReviewDetailIntent.Report(reason, details, reportCommentId)) },
        title = if (reportCommentId == null) Res.string.review_report_title else Res.string.review_comment_report_title,
        policyBody = if (reportCommentId == null) Res.string.review_report_policy_body else Res.string.review_comment_report_policy,
    )
}
