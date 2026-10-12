package com.peto.ramap.data.store

import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultReviewChangeStoreTest {
    @Test
    fun `방출이 합쳐져도 변경 순서를 유지하고 보관 범위 초과를 알린다`() {
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
    fun `요청 시작은 계정 전환을 확인하고 이전 세션의 같은 계정 응답을 거부한다`() {
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
