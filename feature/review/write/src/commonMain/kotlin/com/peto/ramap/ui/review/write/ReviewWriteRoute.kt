package com.peto.ramap.ui.review.write

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.dialog.CommonDialog
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.image.rememberImagesPicker
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.review.write.component.ReviewWriteRouteContent
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_visibility_private
import ramap.shared.generated.resources.profile_visibility_public
import ramap.shared.generated.resources.review_private_profile_confirmation
import ramap.shared.generated.resources.review_private_profile_confirmation_title
import ramap.shared.generated.resources.review_retry
import ramap.shared.generated.resources.shop_review_image_invalid

private const val COMMUNITY_GUIDELINES_URL =
    "https://ramap-orgranization.github.io/Ramap-KMP/community-guidelines.html"

@Composable
fun ShopReviewWriteRoute(
    shopId: String,
    reviewId: String? = null,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onLogin: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: ReviewWriteViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.dispatch(ReviewWriteIntent.Open(shopId, reviewId))
    }
    val imagePicker =
        rememberImagesPicker(
            maxSelectionCount = if (uiState.canEdit) ReviewImage.MAX_COUNT - uiState.images.size - uiState.existingImages.size else 0,
            onImagesPicked = { images ->
                for ((bytes, mimeType) in images) {
                    viewModel.dispatch(ReviewWriteIntent.AddImage(ReviewImage(bytes, mimeType)))
                }
            },
            onRejected = {
                toastManager.tryShow(ToastData(Res.string.shop_review_image_invalid, ToastType.ERROR))
            },
        )
    ObserveAsEvents(viewModel.sideEffect) {
        when (it) {
            ReviewWriteSideEffect.Submitted -> onSubmitted()
            ReviewWriteSideEffect.LoginRequired -> onLogin()
            is ReviewWriteSideEffect.ShowToast -> {
                val action =
                    if (it.canRetry) {
                        ToastAction(Res.string.review_retry) {
                            viewModel.dispatch(if (reviewId == null) ReviewWriteIntent.Load else ReviewWriteIntent.ReloadReview)
                        }
                    } else {
                        null
                    }
                toastManager.show(it.data.copy(action = action))
            }
        }
    }
    ReviewWriteRouteContent(
        state = uiState,
        onBack = onBack,
        onIntent = viewModel::dispatch,
        onPickImage = imagePicker,
        onOpenGuidelines = { ExternalUriOpener.open(COMMUNITY_GUIDELINES_URL) },
    )
    CommonDialog(
        visible = uiState.showPrivateProfileConfirmation,
        confirmText = stringResource(Res.string.profile_visibility_public),
        dismissText = stringResource(Res.string.profile_visibility_private),
        onDismissRequest = { viewModel.dispatch(ReviewWriteIntent.CancelPrivateProfileConfirmation) },
        onDismiss = { viewModel.dispatch(ReviewWriteIntent.ConfirmSubmitWithPrivateProfile) },
        onConfirm = { viewModel.dispatch(ReviewWriteIntent.ConfirmSubmitWithPublicProfile) },
        content = {
            AppText(
                text = stringResource(Res.string.review_private_profile_confirmation_title),
                style = AppTextStyle.T1,
                color = GrayColor.C500,
                textAlign = TextAlign.Center,
            )
            AppText(
                text = stringResource(Res.string.review_private_profile_confirmation),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTextStyle.B2,
                color = GrayColor.C400,
                textAlign = TextAlign.Center,
            )
        },
    )
}
