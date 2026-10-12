package com.peto.ramap.ui.review.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.ui.review.my.contract.MyReviewsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyReviewsViewModelTest {
    @Test
    fun `filter uses saved review visibility and keeps owner counts`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository(), changes = FakeReviewChangeStore())
            runCurrent()
            viewModel.dispatch(MyReviewsIntent.SelectFilter(MyReviewVisibility.PRIVATE))
            runCurrent()
            assertEquals(MyReviewVisibility.PRIVATE, repository.calls.last().second)
            assertEquals(12, viewModel.uiState.value.totalCount)
            assertEquals(10, viewModel.uiState.value.publicCount)
            assertEquals(2, viewModel.uiState.value.privateCount)
            assertFalse(viewModel.uiState.value.profileIsPublic)
        }

    @Test
    fun `passive return preserves sixty loaded reviews without another query`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository(), changes = FakeReviewChangeStore())
            runCurrent()
            viewModel.dispatch(MyReviewsIntent.LoadMore)
            runCurrent()
            assertEquals(60, viewModel.uiState.value.reviews.size)
            val calls = repository.calls.toList()
            val rows = viewModel.uiState.value.reviews
            viewModel.dispatch(MyReviewsIntent.ReturnedToScreen)
            runCurrent()
            assertEquals(calls, repository.calls)
            assertEquals(rows, viewModel.uiState.value.reviews)
            assertFalse(viewModel.uiState.value.failed)
        }

    @Test
    fun `pagination failure retries failed offset while keeping tiles`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository(), changes = FakeReviewChangeStore())
            runCurrent()
            repository.failNext = true
            viewModel.dispatch(MyReviewsIntent.LoadMore)
            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertEquals(30, viewModel.uiState.value.reviews.size)
            viewModel.dispatch(MyReviewsIntent.Retry)
            runCurrent()
            assertEquals(30L, repository.calls.last().first)
            assertEquals(60, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `late filter response does not replace newly selected private grid`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository(), changes = FakeReviewChangeStore())
            runCurrent()
            repository.pendingResults += pending
            viewModel.dispatch(MyReviewsIntent.SelectFilter(MyReviewVisibility.PUBLIC))
            runCurrent()
            viewModel.dispatch(MyReviewsIntent.SelectFilter(MyReviewVisibility.PRIVATE))
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewsPage(emptyList(), 99, 99, 0, true)))
            runCurrent()
            assertEquals(MyReviewVisibility.PRIVATE, viewModel.uiState.value.filter)
            assertEquals(2, viewModel.uiState.value.reviews.size)
            assertEquals(12, viewModel.uiState.value.totalCount)
        }

    @Test
    fun `late first account response cannot replace a new first account epoch after A B A`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val profiles = FakeProfileRepository("first")
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            val viewModel = MyReviewsViewModel(repository, profiles, changes = FakeReviewChangeStore())
            runCurrent()
            repository.pendingResults += pending
            viewModel.dispatch(MyReviewsIntent.Retry)
            runCurrent()
            profiles.sessionUserIds.value = "second"
            runCurrent()
            profiles.sessionUserIds.value = "first"
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewsPage(emptyList(), 99, 99, 0, true)))
            runCurrent()
            assertEquals("first", viewModel.uiState.value.userId)
            assertEquals(12, viewModel.uiState.value.totalCount)
            assertEquals(12, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `two covered owner screens consume retained mutations outside filter without another query`() =
        coroutinesTest {
            val store = FakeReviewChangeStore()
            val firstRepository = FakeOwnerReviews(total = 75)
            val secondRepository = FakeOwnerReviews(total = 75)
            val first = MyReviewsViewModel(firstRepository, FakeProfileRepository("owner"), store)
            val second = MyReviewsViewModel(secondRepository, FakeProfileRepository("owner"), store)
            runCurrent()
            first.dispatch(MyReviewsIntent.LoadMore)
            second.dispatch(MyReviewsIntent.SelectFilter(MyReviewVisibility.PRIVATE))
            runCurrent()
            val firstCalls = firstRepository.calls.size
            val secondCalls = secondRepository.calls.size
            val review =
                com.peto.ramap.domain.model.review
                    .Review(
                        "unloaded-public",
                        "shop",
                        "리뷰 본문",
                        "2020",
                        author =
                            com.peto.ramap.domain.model.community
                                .ReviewAuthor("owner", "작성자"),
                    )
            store.publish(
                com.peto.ramap.domain.model.review.change.ReviewChange
                    .Written(
                        review.id,
                        "owner",
                        "shop",
                        com.peto.ramap.domain.model.review.change
                            .ReviewSnapshot(review, "매장"),
                        null,
                        com.peto.ramap.domain.model.review.change
                            .ReviewOwnerCounts(74, 72, 2, 72, false),
                    ),
            )
            store.publish(
                com.peto.ramap.domain.model.review.change.ReviewChange
                    .Written(
                        review.id,
                        "owner",
                        "shop",
                        com.peto.ramap.domain.model.review.change
                            .ReviewSnapshot(review, "매장"),
                        null,
                        com.peto.ramap.domain.model.review.change
                            .ReviewOwnerCounts(73, 71, 2, 71, false),
                    ),
            )
            runCurrent()
            assertEquals(73, first.uiState.value.totalCount)
            assertEquals(73, second.uiState.value.totalCount)
            assertEquals(71, second.uiState.value.publicCount)
            assertEquals(2, second.uiState.value.privateCount)
            assertEquals(60, first.uiState.value.reviews.size)
            assertEquals(2, second.uiState.value.reviews.size)
            assertEquals(firstCalls, firstRepository.calls.size)
            assertEquals(secondCalls, secondRepository.calls.size)
        }

    @Test
    fun `late append cannot resurrect a deleted review and retries only pending page`() =
        coroutinesTest {
            val store = FakeReviewChangeStore()
            val repository = FakeOwnerReviews(total = 75)
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository("owner"), store)
            runCurrent()
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            repository.pendingResults += pending
            viewModel.dispatch(MyReviewsIntent.LoadMore)
            runCurrent()
            val deleted =
                viewModel.uiState.value.reviews
                    .first()
            repository.removedIds += deleted.id
            val canonical =
                deleted.toReview().copy(
                    author =
                        com.peto.ramap.domain.model.community
                            .ReviewAuthor("owner", "작성자"),
                )
            store.publish(
                com.peto.ramap.domain.model.review.change.ReviewChange
                    .Written(
                        deleted.id,
                        "owner",
                        "shop",
                        com.peto.ramap.domain.model.review.change
                            .ReviewSnapshot(canonical, "매장"),
                        null,
                        com.peto.ramap.domain.model.review.change
                            .ReviewOwnerCounts(74, 72, 2, 72, false),
                    ),
            )
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewsPage(listOf(deleted), 75, 73, 2, false)))
            runCurrent()
            assertFalse(
                viewModel.uiState.value.reviews
                    .any { it.id == deleted.id },
            )
            assertEquals(74, viewModel.uiState.value.totalCount)
            assertEquals(29L, repository.calls.last().first)
            assertEquals(59, viewModel.uiState.value.reviews.size)
        }
}
