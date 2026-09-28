package com.peto.ramap.ui.review.write

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ShopReviewRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteLoadKey
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_load_failed
import ramap.shared.generated.resources.shop_review_failure_message

class ReviewWriteViewModel(
    private val reviewRepository: ShopReviewRepository,
    private val profileRepository: ProfileRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<ReviewWriteUiState, ReviewWriteIntent, ReviewWriteSideEffect>(ReviewWriteUiState()) {
    private var shopId: String? = null

    init {
        viewModelScope.launch {
            profileRepository.sessionUserIds
                .distinctUntilChanged()
                .collect {
                    cancelTask(SUBMIT_TASK)
                    reduce {
                        ReviewWriteUiState(
                            shop = shop,
                            shopLoadFailed = shopLoadFailed,
                            loadState = loadState,
                        )
                    }
                }
        }
    }

    override suspend fun handleIntent(intent: ReviewWriteIntent) {
        when (intent) {
            is ReviewWriteIntent.Open -> openShop(intent.shopId)
            is ReviewWriteIntent.ChangeBody -> changeBody(intent.body)
            is ReviewWriteIntent.ChangeVisibility -> if (!currentState.isSubmitting) reduce { copy(isPublic = intent.isPublic) }
            is ReviewWriteIntent.AddImage -> addImage(intent.image)
            is ReviewWriteIntent.RemoveImage -> if (!currentState.isSubmitting) reduce { copy(images = images.filterIndexed { index, _ -> index != intent.index }) }
            is ReviewWriteIntent.MoveImage -> moveImage(intent.fromIndex, intent.toIndex)
            ReviewWriteIntent.Load -> {
                loadShop()
            }

            ReviewWriteIntent.Submit -> submit()
        }
    }

    private fun openShop(shopId: String) {
        if (this.shopId == shopId) return
        cancelTask(SHOP_TASK)
        cancelTask(SUBMIT_TASK)
        this.shopId = shopId
        reduce { ReviewWriteUiState() }
        loadShop()
    }

    private fun changeBody(body: String) {
        if (currentState.isSubmitting) return
        reduce { copy(body = limitBodyLength(body)) }
    }

    private fun addImage(image: ReviewImage) {
        if (!currentState.isSubmitting && image.isValid() && currentState.images.size < ReviewImage.MAX_COUNT) {
            reduce { copy(images = images + image) }
        }
    }

    private fun moveImage(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (currentState.isSubmitting) return
        if (fromIndex !in currentState.images.indices || toIndex !in currentState.images.indices) return
        reduce {
            copy(
                images = images.toMutableList().apply { add(toIndex, removeAt(fromIndex)) },
            )
        }
    }

    private fun loadShop() {
        val shopId = shopId ?: return
        launchResultTask(
            taskKey = SHOP_TASK,
            loadKey = ReviewWriteLoadKey.Shop,
            onStart = { copy(shopLoadFailed = false) },
            request = { ramenShopRepository.fetchShopDetail(shopId) },
            onSuccess = { detail -> reduce { copy(shop = detail.shop) } },
            onError = {
                reduce { copy(shopLoadFailed = true) }
                postSideEffect(
                    ReviewWriteSideEffect.ShowToast(
                        ToastData(Res.string.review_load_failed, ToastType.ERROR),
                        canRetry = true,
                    ),
                )
            },
        )
    }

    private suspend fun submit() {
        if (!currentState.canSubmit) return
        if (profileRepository.sessionUserIds.first() == null) {
            postSideEffect(ReviewWriteSideEffect.LoginRequired)
            return
        }
        val shopId = shopId ?: return
        val draft = currentState
        launchTask(
            taskKey = SUBMIT_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
        ) {
            when (reviewRepository.submitReview(shopId, draft.body, draft.images, isPublic = draft.isPublic)) {
                is RamapResult.Error -> {
                    showSubmitFailureIfAuthenticated()
                }

                is RamapResult.Success -> {
                    reduce { copy(body = "", images = emptyList(), isPublic = true) }
                    postSideEffect(ReviewWriteSideEffect.Submitted)
                }
            }
        }
    }

    private suspend fun showSubmitFailureIfAuthenticated() {
        postSideEffect(
            ReviewWriteSideEffect.ShowToast(
                ToastData(Res.string.shop_review_failure_message, ToastType.ERROR),
            ),
        )
    }

    private companion object {
        const val SHOP_TASK = "shop-review-write-shop"
        const val SUBMIT_TASK = "shop-review-write-submit"

        fun limitBodyLength(body: String): String {
            var endIndex = 0
            var characterCount = 0
            while (endIndex < body.length && characterCount < Review.BODY_LENGTH.last) {
                endIndex +=
                    if (body[endIndex].isHighSurrogate() && body.getOrNull(endIndex + 1)?.isLowSurrogate() == true) {
                        2
                    } else {
                        1
                    }
                characterCount++
            }
            return body.substring(0, endIndex)
        }
    }
}
