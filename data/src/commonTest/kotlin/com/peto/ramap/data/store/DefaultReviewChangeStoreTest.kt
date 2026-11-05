package com.peto.ramap.data.store

import com.peto.ramap.domain.model.review.ReviewLike
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultReviewChangeStoreTest {
    @Test
    fun `좋아요는 사진을 유지하고 공개 범위 변경과 리뷰 삭제는 사진 재사용 범위를 갱신한다`() {
        val sessions = MutableStateFlow<String?>("viewer")
        val store = DefaultReviewChangeStore(sessions, { sessions.value })
        val epoch = store.synchronizeSession()
        store.publish(ReviewChange.Liked("review", ReviewLike(1, true)), epoch)
        assertEquals(0L, store.journal.value.imageRevision)
        store.publish(ReviewChange.AccessChanged("owner"), epoch)
        val changed = store.journal.value.imageRevision
        assertTrue(changed > 0)
        store.publish(ReviewChange.Liked("review", ReviewLike(2, true)), epoch)
        assertEquals(changed, store.journal.value.imageRevision)
        store.publish(
            ReviewChange.Written(
                reviewId = "review",
                userId = "owner",
                shopId = "shop",
                before = null,
                after = null,
                counts = ReviewOwnerCounts(0, 0, 0, 0, true),
            ),
            epoch,
        )
        assertTrue(store.journal.value.imageRevision > changed)
    }

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
