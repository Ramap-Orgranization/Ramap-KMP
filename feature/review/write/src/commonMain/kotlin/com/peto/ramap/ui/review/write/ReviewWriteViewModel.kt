package com.peto.ramap.ui.review.write

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.repository.RamenShopRepository
import com.peto.ramap.domain.repository.ReviewRepository
import com.peto.ramap.ui.base.BaseViewModel
import com.peto.ramap.ui.review.write.contract.ExistingReviewPhoto
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteLoadKey
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import com.peto.ramap.ui.task.TaskPolicy
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_load_failed
import ramap.shared.generated.resources.shop_review_failure_message

class ReviewWriteViewModel(
    private val reviewRepository: ReviewRepository,
    private val profileRepository: ProfileRepository,
    private val ramenShopRepository: RamenShopRepository,
) : BaseViewModel<ReviewWriteUiState, ReviewWriteIntent, ReviewWriteSideEffect>(ReviewWriteUiState()) {
    private var shopId: String? = null
    private var reviewId: String? = null

    init {
        viewModelScope.launch {
            profileRepository.sessionUserIds
                .distinctUntilChanged()
                .collect { userId ->
                    cancelTask(PROFILE_VISIBILITY_TASK)
                    cancelTask(SUBMIT_TASK)
                    cancelTask(REVIEW_TASK)
                    reduce {
                        ReviewWriteUiState(
                            reviewId = reviewId,
                            shop = shop,
                            shopLoadFailed = shopLoadFailed,
                            loadState = loadState,
                        )
                    }
                    if (userId != null && reviewId != null) loadEditableReview()
                }
        }
    }

    override suspend fun handleIntent(intent: ReviewWriteIntent) {
        when (intent) {
            is ReviewWriteIntent.Open -> openShop(intent.shopId, intent.reviewId)
            is ReviewWriteIntent.ChangeBody -> changeBody(intent.body)
            is ReviewWriteIntent.ChangeVisibility -> if (currentState.canEdit) reduce { copy(isPublic = intent.isPublic) }
            is ReviewWriteIntent.AddImage -> addImage(intent.image)
            is ReviewWriteIntent.RemoveImage -> if (currentState.canEdit) reduce { copy(images = images.filterIndexed { index, _ -> index != intent.index }) }
            is ReviewWriteIntent.RemoveExistingImage ->
                if (currentState.canEdit) {
                    reduce {
                        copy(existingImages = existingImages.filterIndexed { index, _ -> index != intent.index })
                    }
                }
            ReviewWriteIntent.ReloadReview -> loadEditableReview()
            is ReviewWriteIntent.MoveImage -> moveImage(intent.fromIndex, intent.toIndex)
            ReviewWriteIntent.Load -> {
                loadShop()
            }

            ReviewWriteIntent.Submit -> submit()
            ReviewWriteIntent.ConfirmSubmitWithPrivateProfile -> confirmSubmitWithPrivateProfile()
            ReviewWriteIntent.ConfirmSubmitWithPublicProfile -> confirmSubmitWithPublicProfile()
            ReviewWriteIntent.CancelPrivateProfileConfirmation ->
                reduce { copy(showPrivateProfileConfirmation = false) }
        }
    }

    private fun openShop(
        shopId: String,
        reviewId: String?,
    ) {
        if (this.shopId == shopId && this.reviewId == reviewId) return
        cancelTask(SHOP_TASK)
        cancelTask(PROFILE_VISIBILITY_TASK)
        cancelTask(SUBMIT_TASK)
        cancelTask(REVIEW_TASK)
        this.shopId = shopId
        this.reviewId = reviewId
        reduce { ReviewWriteUiState(reviewId = reviewId) }
        loadShop()
        if (reviewId != null) loadEditableReview()
    }

    private fun changeBody(body: String) {
        if (!currentState.canEdit) return
        reduce { copy(body = limitBodyLength(body)) }
    }

    private fun addImage(image: ReviewImage) {
        if (currentState.canEdit && image.isValid() && currentState.images.size + currentState.existingImages.size < ReviewImage.MAX_COUNT) {
            reduce { copy(images = images + image) }
        }
    }

    private fun moveImage(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (!currentState.canEdit) return
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

    private fun loadEditableReview() {
        val reviewId = reviewId ?: return
        launchResultTask(
            taskKey = REVIEW_TASK,
            loadKey = ReviewWriteLoadKey.Review,
            policy = TaskPolicy.CancelPrevious,
            onStart = { copy(reviewLoadFailed = false, reviewLoaded = false) },
            request = { reviewRepository.fetchEditableReview(reviewId) },
            onSuccess = { review ->
                if (!currentCoroutineContext().isActive || this.reviewId != reviewId) {
                    return@launchResultTask
                }
                if (review == null || review.shopId != shopId) {
                    reduce { copy(reviewLoadFailed = true) }
                    postSideEffect(
                        ReviewWriteSideEffect.ShowToast(
                            ToastData(Res.string.review_load_failed, ToastType.ERROR),
                            canRetry = true,
                        ),
                    )
                    return@launchResultTask
                }
                reduce {
                    copy(
                        reviewLoaded = true,
                        body = review.body,
                        isPublic = review.isPublic,
                        existingImages =
                            review.imagePaths.zip(review.imageUrls).map { (path, url) ->
                                ExistingReviewPhoto(path, url)
                            },
                        images = emptyList(),
                    )
                }
            },
            onError = {
                if (!currentCoroutineContext().isActive) return@launchResultTask
                reduce { copy(reviewLoadFailed = true) }
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
        val userId = profileRepository.sessionUserIds.first()
        if (userId == null) {
            postSideEffect(ReviewWriteSideEffect.LoginRequired)
            return
        }
        val draft = currentState
        if (!draft.isPublic) {
            submitDraft(draft)
            return
        }
        launchResultTask(
            taskKey = PROFILE_VISIBILITY_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = { profileRepository.fetchMyProfile() },
            onSuccess = { profile ->
                if (profile.userId != userId || profileRepository.sessionUserIds.first() != userId) {
                    return@launchResultTask
                }
                if (draft.isPublic && !profile.isPublic) {
                    reduce { copy(showPrivateProfileConfirmation = true) }
                } else {
                    submitDraft(draft)
                }
            },
            onError = { showSubmitFailureIfAuthenticated() },
        )
    }

    private fun confirmSubmitWithPrivateProfile() {
        if (!currentState.showPrivateProfileConfirmation || !Review.isValidBody(currentState.body)) return
        val draft = currentState.copy(showPrivateProfileConfirmation = false)
        reduce { copy(showPrivateProfileConfirmation = false) }
        submitDraft(draft)
    }

    private fun confirmSubmitWithPublicProfile() {
        if (!currentState.showPrivateProfileConfirmation || !Review.isValidBody(currentState.body)) return
        val draft = currentState.copy(showPrivateProfileConfirmation = false)
        reduce { copy(showPrivateProfileConfirmation = false) }
        launchResultTask(
            taskKey = UPDATE_PROFILE_VISIBILITY_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = { profileRepository.updateProfileVisibility(isPublic = true) },
            onSuccess = {
                submitDraft(draft)
            },
            onError = {
                showSubmitFailureIfAuthenticated()
            },
        )
    }

    private fun submitDraft(draft: ReviewWriteUiState) {
        val shopId = shopId ?: return
        val editReviewId = reviewId
        launchTask(
            taskKey = SUBMIT_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
        ) {
            val result =
                if (editReviewId == null) {
                    reviewRepository.submitReview(shopId, draft.body, draft.images, isPublic = draft.isPublic)
                } else {
                    reviewRepository.updateReview(
                        reviewId = editReviewId,
                        body = draft.body,
                        retainedImagePaths = draft.existingImages.map { it.path },
                        newImages = draft.images,
                        isPublic = draft.isPublic,
                    )
                }
            if (!currentCoroutineContext().isActive) return@launchTask
            when (result) {
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
        const val PROFILE_VISIBILITY_TASK = "shop-review-write-profile-visibility"
        const val UPDATE_PROFILE_VISIBILITY_TASK = "shop-review-write-update-profile-visibility"
        const val SUBMIT_TASK = "shop-review-write-submit"
        const val REVIEW_TASK = "shop-review-write-review"

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
