package com.peto.ramap.fake

import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeJournal
import com.peto.ramap.domain.model.review.change.RevisionedReviewChange
import com.peto.ramap.domain.store.ReviewChangeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeReviewChangeStore : ReviewChangeStore {
    private val state = MutableStateFlow(ReviewChangeJournal(userId = "owner"))
    override val journal = state.asStateFlow()

    fun publish(change: ReviewChange) {
        val current = state.value
        val revision = current.revision + 1
        state.value = current.copy(revision = revision, changes = (current.changes + RevisionedReviewChange(revision, change)).takeLast(128))
    }

    fun reset(userId: String?) {
        state.value = ReviewChangeJournal(epoch = state.value.epoch + 1, userId = userId)
    }
}
