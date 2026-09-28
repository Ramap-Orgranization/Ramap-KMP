package com.peto.ramap.ui.review.write

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastAction
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.platform.image.rememberImagesPicker
import com.peto.ramap.ui.base.ObserveAsEvents
import com.peto.ramap.ui.review.write.component.ReviewWriteRouteContent
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_retry
import ramap.shared.generated.resources.shop_review_image_invalid

@Composable
fun ShopReviewWriteRoute(
    shopId: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onLogin: () -> Unit,
    toastManager: ToastManager = koinInject(),
    viewModel: ReviewWriteViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(shopId) {
        viewModel.dispatch(ReviewWriteIntent.Open(shopId))
    }
    val imagePicker =
        rememberImagesPicker(
            maxSelectionCount = ReviewImage.MAX_COUNT - state.images.size,
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
                        ToastAction(Res.string.review_retry) { viewModel.dispatch(ReviewWriteIntent.Load) }
                    } else {
                        null
                    }
                toastManager.show(it.data.copy(action = action))
            }
        }
    }
    ReviewWriteRouteContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::dispatch,
        onPickImage = imagePicker,
    )
}
