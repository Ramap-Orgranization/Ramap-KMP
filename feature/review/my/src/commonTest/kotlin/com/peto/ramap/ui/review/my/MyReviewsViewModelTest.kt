package com.peto.ramap.ui.review.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.MyReviewVisibility
import com.peto.ramap.domain.model.review.MyReviewsPage
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
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
    fun `필터는 저장된 공개 여부를 사용하며 작성자 집계를 유지한다`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = FakeProfileRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
    fun `변경 없이 복귀하면 재조회 없이 기존 리뷰 예순 개를 유지한다`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = FakeProfileRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
    fun `다음 페이지 조회 실패 시 기존 항목을 유지하며 실패한 위치를 다시 조회한다`() =
        coroutinesTest {
            val repository = FakeOwnerReviews(total = 75)
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = FakeProfileRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
    fun `늦은 필터 응답은 새로 선택한 비공개 리뷰 목록을 덮어쓰지 않는다`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = FakeProfileRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
    fun `계정을 전환했다가 돌아온 뒤 이전 세션의 늦은 응답은 새 세션을 덮어쓰지 않는다`() =
        coroutinesTest {
            val repository = FakeOwnerReviews()
            val profiles = FakeProfileRepository("first")
            val pending = CompletableDeferred<RamapResult<MyReviewsPage>>()
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = profiles,
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            repository.pendingResults += pending
            viewModel.dispatch(MyReviewsIntent.Retry)
            runCurrent()
            profiles.sessionUserId.value = "second"
            runCurrent()
            profiles.sessionUserId.value = "first"
            runCurrent()
            pending.complete(RamapResult.Success(MyReviewsPage(emptyList(), 99, 99, 0, true)))
            runCurrent()
            assertEquals("first", viewModel.uiState.value.userId)
            assertEquals(12, viewModel.uiState.value.totalCount)
            assertEquals(12, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `두 작성자 화면은 필터 밖의 변경도 재조회 없이 반영한다`() =
        coroutinesTest {
            val store = FakeReviewChangeStore()
            val firstRepository = FakeOwnerReviews(total = 75)
            val secondRepository = FakeOwnerReviews(total = 75)
            val first =
                MyReviewsViewModel(
                    reviews = firstRepository,
                    profiles = FakeProfileRepository("owner"),
                    changes = store,
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            val second =
                MyReviewsViewModel(
                    reviews = secondRepository,
                    profiles = FakeProfileRepository("owner"),
                    changes = store,
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
    fun `늦은 다음 페이지 응답은 삭제된 리뷰를 복원하지 않으며 미완료 페이지만 재시도한다`() =
        coroutinesTest {
            val store = FakeReviewChangeStore()
            val repository = FakeOwnerReviews(total = 75)
            val viewModel =
                MyReviewsViewModel(
                    reviews = repository,
                    profiles = FakeProfileRepository("owner"),
                    changes = store,
                    reviewChangeReducer = ReviewChangeReducer(),
                )
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
