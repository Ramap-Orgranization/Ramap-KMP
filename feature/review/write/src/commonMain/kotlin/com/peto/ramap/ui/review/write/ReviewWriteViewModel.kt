package com.peto.ramap.ui.review.write

import androidx.lifecycle.viewModelScope
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.EditableReview
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
    private var submissionGeneration = 0L

    init {
        observeSessionUser()
    }

    override suspend fun handleIntent(intent: ReviewWriteIntent) {
        when (intent) {
            is ReviewWriteIntent.Open -> openShop(intent.shopId, intent.reviewId)
            is ReviewWriteIntent.ChangeBody -> changeBody(intent.body)
            is ReviewWriteIntent.ChangeVisibility -> changeVisibility(intent.isPublic)
            is ReviewWriteIntent.AddImage -> addImage(intent.image)
            is ReviewWriteIntent.RemoveImage -> removeImage(intent.index)
            is ReviewWriteIntent.RemoveExistingImage -> removeExistingImage(intent.index)
            is ReviewWriteIntent.MoveImage -> moveImage(intent.fromIndex, intent.toIndex)
            ReviewWriteIntent.ReloadReview -> loadEditableReview()
            ReviewWriteIntent.Load -> loadShop()
            ReviewWriteIntent.Submit -> submit()
            ReviewWriteIntent.ConfirmSubmitWithPrivateProfile -> confirmSubmitWithPrivateProfile()
            ReviewWriteIntent.ConfirmSubmitWithPublicProfile -> confirmSubmitWithPublicProfile()
            ReviewWriteIntent.CancelPrivateProfileConfirmation -> cancelPrivateProfileConfirmation()
        }
    }

    private fun observeSessionUser() {
        viewModelScope.launch {
            profileRepository.sessionUserId
                .distinctUntilChanged()
                .collect { userId ->
                    resetStateForSessionUserChange()
                    val hasUserAndReview = userId != null && reviewId != null
                    if (hasUserAndReview) {
                        loadEditableReview()
                    }
                }
        }
    }

    private fun resetStateForSessionUserChange() {
        submissionGeneration++
        cancelTask(PROFILE_VISIBILITY_TASK)
        cancelTask(UPDATE_PROFILE_VISIBILITY_TASK)
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
    }

    private fun openShop(
        shopId: String,
        reviewId: String?,
    ) {
        submissionGeneration++
        cancelTask(SHOP_TASK)
        cancelTask(PROFILE_VISIBILITY_TASK)
        cancelTask(UPDATE_PROFILE_VISIBILITY_TASK)
        cancelTask(SUBMIT_TASK)
        cancelTask(REVIEW_TASK)
        this.shopId = shopId
        this.reviewId = reviewId
        reduce { ReviewWriteUiState(reviewId = reviewId) }
        loadShop()
        val hasReview = reviewId != null
        if (hasReview) loadEditableReview()
    }

    private fun changeBody(body: String) {
        val cannotEdit = !currentState.canEdit
        if (cannotEdit) return
        val exceedsBodyLength = Review.bodyCharacterCount(body) > Review.BODY_LENGTH.last
        if (exceedsBodyLength) return
        reduce { copy(body = body) }
    }

    private fun changeVisibility(isPublic: Boolean) {
        val cannotEdit = !currentState.canEdit
        if (cannotEdit) return
        reduce { copy(isPublic = isPublic) }
    }

    private fun addImage(image: ReviewImage) {
        val cannotEditOrInvalid = !currentState.canEdit || !image.isValid()
        if (cannotEditOrInvalid) return
        val currentTotalCount = currentState.images.size + currentState.existingImages.size
        val canAddMore = currentTotalCount < ReviewImage.MAX_COUNT
        if (canAddMore) {
            reduce { copy(images = images + image) }
        }
    }

    private fun removeImage(index: Int) {
        val cannotEdit = !currentState.canEdit
        if (cannotEdit) return
        reduce { copy(images = images.filterIndexed { i, _ -> i != index }) }
    }

    private fun removeExistingImage(index: Int) {
        val cannotEdit = !currentState.canEdit
        if (cannotEdit) return
        reduce { copy(existingImages = existingImages.filterIndexed { i, _ -> i != index }) }
    }

    private fun moveImage(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val cannotEdit = !currentState.canEdit
        if (cannotEdit) return
        val isFromIndexInvalid = fromIndex !in currentState.images.indices
        val isToIndexInvalid = toIndex !in currentState.images.indices
        if (isFromIndexInvalid || isToIndexInvalid) return
        reduce {
            copy(
                images = images.toMutableList().apply { add(toIndex, removeAt(fromIndex)) },
            )
        }
    }

    private fun cancelPrivateProfileConfirmation() {
        reduce { copy(showPrivateProfileConfirmation = false) }
    }

    private fun loadShop() {
        val shopId = shopId ?: return
        launchResultTask(
            taskKey = SHOP_TASK,
            loadKey = ReviewWriteLoadKey.Shop,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(shopLoadFailed = false) },
            request = { ramenShopRepository.fetchShopOverview(shopId) },
            onSuccess = { detail -> reduce { copy(shop = detail.shop) } },
            onError = { handleShopLoadError() },
        )
    }

    private suspend fun handleShopLoadError() {
        reduce { copy(shopLoadFailed = true) }
        postSideEffect(
            ReviewWriteSideEffect.ShowToast(
                ToastData(Res.string.review_load_failed, ToastType.ERROR),
                canRetry = true,
            ),
        )
    }

    private fun loadEditableReview() {
        val reviewId = reviewId ?: return
        launchResultTask(
            taskKey = REVIEW_TASK,
            loadKey = ReviewWriteLoadKey.Review,
            policy = TaskPolicy.IgnoreNew,
            onStart = { copy(reviewLoadFailed = false, reviewLoaded = false) },
            request = { reviewRepository.fetchEditableReview(reviewId) },
            onSuccess = { review -> handleEditableReviewSuccess(review, reviewId) },
            onError = { handleReviewLoadError() },
        )
    }

    private suspend fun handleEditableReviewSuccess(
        review: EditableReview?,
        targetReviewId: String,
    ) {
        val isInactive = !currentCoroutineContext().isActive
        val isReviewIdChanged = reviewId != targetReviewId
        if (isInactive || isReviewIdChanged) return
        val isInvalidReviewOrShopMismatch = review == null || review.shopId != shopId
        if (isInvalidReviewOrShopMismatch) {
            handleReviewLoadError()
            return
        }
        val existingPhotos =
            review.imagePaths.zip(review.imageUrls).map { (path, url) ->
                ExistingReviewPhoto(path, url)
            }
        reduce {
            copy(
                reviewLoaded = true,
                body = review.body,
                isPublic = review.isPublic,
                existingImages = existingPhotos,
                images = emptyList(),
            )
        }
    }

    private suspend fun handleReviewLoadError() {
        val isInactive = !currentCoroutineContext().isActive
        if (isInactive) return
        reduce { copy(reviewLoadFailed = true) }
        postSideEffect(
            ReviewWriteSideEffect.ShowToast(
                ToastData(Res.string.review_load_failed, ToastType.ERROR),
                canRetry = true,
            ),
        )
    }

    private suspend fun submit() {
        val cannotSubmit = !currentState.canSubmit
        if (cannotSubmit) return
        val userId = profileRepository.sessionUserId.first()
        val isLoggedOut = userId == null
        if (isLoggedOut) {
            postSideEffect(ReviewWriteSideEffect.LoginRequired)
            return
        }
        val draft = currentState
        val isPrivateDraft = !draft.isPublic
        if (isPrivateDraft) {
            submitDraft(draft)
            return
        }
        checkProfileVisibilityAndSubmit(draft, userId)
    }

    private fun checkProfileVisibilityAndSubmit(
        draft: ReviewWriteUiState,
        userId: String,
    ) {
        val generation = submissionGeneration
        launchResultTask(
            taskKey = PROFILE_VISIBILITY_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = { profileRepository.fetchMyProfile() },
            onSuccess = { profile ->
                val isStaleGeneration = generation != submissionGeneration
                val isUserChanged = profile.userId != userId || profileRepository.sessionUserId.first() != userId
                if (isStaleGeneration || isUserChanged) {
                    return@launchResultTask
                }
                val isPublicReviewWithPrivateProfile = draft.isPublic && !profile.isPublic
                if (isPublicReviewWithPrivateProfile) {
                    reduce { copy(showPrivateProfileConfirmation = true) }
                } else {
                    submitDraft(draft)
                }
            },
            onError = { showSubmitFailureIfAuthenticated() },
        )
    }

    private fun confirmSubmitWithPrivateProfile() {
        val isInvalidState = !hasValidConfirmationState()
        if (isInvalidState) return
        val draft = currentState.copy(showPrivateProfileConfirmation = false)
        reduce { copy(showPrivateProfileConfirmation = false) }
        submitDraft(draft)
    }

    private fun confirmSubmitWithPublicProfile() {
        val isInvalidState = !hasValidConfirmationState()
        if (isInvalidState) return
        val draft = currentState.copy(showPrivateProfileConfirmation = false)
        reduce { copy(showPrivateProfileConfirmation = false) }
        updateProfileVisibilityAndSubmit(draft)
    }

    private fun hasValidConfirmationState(): Boolean {
        val isConfirmationShown = currentState.showPrivateProfileConfirmation
        val isValidBody = Review.isValidBody(currentState.body)
        return isConfirmationShown && isValidBody
    }

    private fun updateProfileVisibilityAndSubmit(draft: ReviewWriteUiState) {
        val generation = submissionGeneration
        launchResultTask(
            taskKey = UPDATE_PROFILE_VISIBILITY_TASK,
            loadKey = ReviewWriteLoadKey.Submit,
            policy = TaskPolicy.IgnoreNew,
            request = { profileRepository.updateProfileVisibility(isPublic = true) },
            onSuccess = {
                val isSameGeneration = generation == submissionGeneration
                if (isSameGeneration) submitDraft(draft)
            },
            onError = { showSubmitFailureIfAuthenticated() },
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
            val result = executeSubmitRequest(shopId, editReviewId, draft)
            val isInactive = !currentCoroutineContext().isActive
            if (isInactive) return@launchTask
            handleSubmitResult(result)
        }
    }

    private suspend fun executeSubmitRequest(
        shopId: String,
        editReviewId: String?,
        draft: ReviewWriteUiState,
    ): RamapResult<Unit> =
        if (editReviewId == null) {
            reviewRepository.submitReview(
                shopId = shopId,
                body = draft.body,
                images = draft.images,
                isPublic = draft.isPublic,
            )
        } else {
            reviewRepository.updateReview(
                reviewId = editReviewId,
                body = draft.body,
                retainedImagePaths = draft.existingImages.map { it.path },
                newImages = draft.images,
                isPublic = draft.isPublic,
            )
        }

    private suspend fun handleSubmitResult(result: RamapResult<Unit>) {
        when (result) {
            is RamapResult.Error -> showSubmitFailureIfAuthenticated()
            is RamapResult.Success -> {
                reduce { copy(body = "", images = emptyList(), isPublic = true) }
                postSideEffect(ReviewWriteSideEffect.Submitted)
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
    }
}
