package com.peto.ramap.data.store

import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultReviewChangeStoreTest {
    @Test
    fun `conflated delivery retains revisions and exposes bounded overflow`() {
        val sessions = MutableStateFlow<String?>("owner")
        val store = DefaultReviewChangeStore(sessions, { sessions.value })
        val epoch = store.synchronizeSession()
        repeat(130) { store.publish(ReviewChange.Liked("$it", ReviewLike(it, true)), epoch) }
        val journal = store.journal.value
        assertEquals(130L, journal.revision)
        assertEquals(128, journal.changes.size)
        assertTrue(journal.hasGapAfter(0))
        assertEquals(128, journal.changesAfter(2).size)
    }

    @Test
    fun `request start observes account switch and rejects old same account epoch completion`() {
        val sessions = MutableStateFlow<String?>("first")
        val store = DefaultReviewChangeStore(sessions, { sessions.value })
        val firstEpoch = store.synchronizeSession()
        sessions.value = "second"
        store.synchronizeSession()
        sessions.value = "first"
        val latestEpoch = store.synchronizeSession()
        store.publish(ReviewChange.Liked("stale", ReviewLike(1, true)), firstEpoch)
        assertTrue(
            store.journal.value.changes
                .isEmpty(),
        )
        store.publish(ReviewChange.Liked("current", ReviewLike(2, true)), latestEpoch)
        assertEquals(
            "current",
            (
                store.journal.value.changes
                    .single()
                    .change as ReviewChange.Liked
            ).reviewId,
        )
    }
}
