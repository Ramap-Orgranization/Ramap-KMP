package com.peto.ramap.ui.review.write

import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.review.write.contract.ReviewWriteLoadKey
import com.peto.ramap.ui.review.write.contract.ReviewWriteUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewWriteUiStateTest {
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
