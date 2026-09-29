package com.peto.ramap.ui.profile.review

import com.peto.ramap.ui.profile.review.contract.ProfileReviewUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileReviewUiStateTest {
    @Test
    fun initialState_canLoadFirstPageAndRetryAfterFailure() {
        assertTrue(ProfileReviewUiState().hasMore)
        assertFalse(ProfileReviewUiState().isLoading)
        assertTrue(ProfileReviewUiState(loadFailed = true).loadFailed)
    }
}
