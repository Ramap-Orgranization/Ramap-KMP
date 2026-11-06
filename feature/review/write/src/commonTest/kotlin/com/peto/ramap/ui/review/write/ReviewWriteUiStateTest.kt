package com.peto.ramap.ui.review.write

import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.review.write.contract.ExistingReviewPhoto
import com.peto.ramap.ui.review.write.contract.ReviewWriteLoadKey
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewWriteUiStateTest {
    @Test
    fun `신규 사진이나 기존 사진이 있어도 최소 5자 본문이 있어야 제출할 수 있다`() {
        val newPhoto = ReviewWriteUiState(images = listOf(ReviewImage(byteArrayOf(1), ReviewImage.JPEG_MIME_TYPE)))
        val existingPhoto = ReviewWriteUiState(existingImages = listOf(ExistingReviewPhoto("path", null)))
        for (state in listOf(newPhoto, existingPhoto)) {
            assertFalse(state.canSubmit)
            assertFalse(state.copy(body = "  \t\n").canSubmit)
            assertFalse(state.copy(body = "1234").canSubmit)
            val ready = state.copy(body = "12345")
            assertTrue(ready.canSubmit)
            assertFalse(ready.copy(loadState = LoadState.loading(ReviewWriteLoadKey.Submit)).canSubmit)
            assertFalse(ready.copy(showPrivateProfileConfirmation = true).canSubmit)
        }
        assertFalse(ReviewWriteUiState().canSubmit)
    }

    @Test
    fun `유효한 본문이 있어도 수정 데이터를 불러오기 전에는 제출할 수 없다`() {
        val state = ReviewWriteUiState(body = "12345", reviewId = "review", existingImages = listOf(ExistingReviewPhoto("path", null)))
        assertFalse(state.canSubmit)
        assertTrue(state.copy(reviewLoaded = true).canSubmit)
        assertFalse(state.copy(reviewLoaded = true, reviewLoadFailed = true).canSubmit)
    }

    @Test
    fun canSubmit_requiresValidTextAndNotSubmitting() {
        val ready = ReviewWriteUiState(body = "12345")
        assertTrue(ready.canSubmit)
        assertFalse(ready.copy(body = "1234").canSubmit)
        assertFalse(ready.copy(loadState = LoadState.loading(ReviewWriteLoadKey.Submit)).canSubmit)
    }

    @Test
    fun reviewVisibility_defaultsToPublic() {
        assertTrue(ReviewWriteUiState().isPublic)
    }
}
