package com.peto.ramap.ui.review.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.fake.FakeProfileRepository
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
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository())
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
    fun `failed notifier refresh retries from zero and restores loaded extent`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(MyReviewsIntent.LoadMore)
            runCurrent()
            assertEquals(60, viewModel.uiState.value.reviews.size)
            repository.failNext = true
            repository.changed.emit(Unit)
            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertEquals(60, viewModel.uiState.value.reviews.size)
            val callBeforeRetry = repository.calls.size
            viewModel.dispatch(MyReviewsIntent.Retry)
            runCurrent()
            assertEquals(0L, repository.calls[callBeforeRetry].first)
            assertEquals(60, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `pagination failure retries failed offset while keeping tiles`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository())
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
            val viewModel = MyReviewsViewModel(repository, FakeProfileRepository())
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
    fun `late previous account response cannot replace current account reviews`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val profiles = FakeProfileRepository("first")
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            val viewModel = MyReviewsViewModel(repository, profiles)
            runCurrent()
            repository.pendingResults += pending
            viewModel.dispatch(MyReviewsIntent.Retry)
            runCurrent()
            profiles.sessionUserIds.value = "second"
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewsPage(emptyList(), 99, 99, 0, true)))
            runCurrent()
            assertEquals("second", viewModel.uiState.value.userId)
            assertEquals(12, viewModel.uiState.value.totalCount)
            assertEquals(12, viewModel.uiState.value.reviews.size)
        }
}
