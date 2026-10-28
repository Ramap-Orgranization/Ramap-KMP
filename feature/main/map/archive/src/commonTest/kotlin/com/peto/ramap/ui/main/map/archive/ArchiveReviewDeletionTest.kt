package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.review.ArchiveShops
import com.peto.ramap.domain.model.review.MyReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.domain.model.review.change.ReviewOwnerCounts
import com.peto.ramap.domain.model.review.change.ReviewSnapshot
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveReviewDeletionTest {
    @Test
    fun `삭제 취소와 선택되지 않은 리뷰 요청은 서버를 변경하지 않는다`() =
        coroutinesTest {
            val reviews = FakeArchiveReviewRepository()
            val viewModel = ArchiveMapViewModel(visits(row("review")), FakeReviewChangeStore(), ReviewChangeReducer(), reviews)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("missing"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("review"))
            runCurrent()
            assertEquals("review", viewModel.uiState.value.deleteTargetReviewId)
            viewModel.dispatch(ArchiveMapIntent.DismissReviewDeletion)
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            assertTrue(reviews.deletedReviewIds.isEmpty())
            assertEquals(1, viewModel.uiState.value.selectedReviews.size)
        }

    @Test
    fun `삭제 성공은 기록 수를 줄이고 마지막 기록 삭제는 상세와 마커를 제거한다`() =
        coroutinesTest {
            val first = row("first")
            val second = row("second")
            val changes = FakeReviewChangeStore()
            val repository = visits(first, second)
            val reviews = FakeArchiveReviewRepository()
            reviews.delete = { id ->
                changes.publish(deleted(if (id == first.review.id) first else second))
                RamapResult.Success(Unit)
            }
            val viewModel = ArchiveMapViewModel(repository, changes, ReviewChangeReducer(), reviews)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("first"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertEquals(
                listOf("second"),
                viewModel.uiState.value.selectedReviews
                    .map { it.review.id },
            )
            assertEquals("shop", viewModel.uiState.value.selectedShopId)
            assertEquals(setOf("shop"), viewModel.uiState.value.shops.keys)
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("second"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertNull(viewModel.uiState.value.selectedShopId)
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            assertTrue(
                viewModel.uiState.value.selectedReviews
                    .isEmpty(),
            )
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isDeletingReview)
            assertEquals(listOf("first", "second"), reviews.deletedReviewIds)
            assertEquals(1, repository.requests)
        }

    @Test
    fun `삭제 중 연속 확인과 다른 리뷰 선택은 중복 요청을 만들지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<Unit>>()
            val reviews = FakeArchiveReviewRepository().apply { delete = { pending.await() } }
            val viewModel = ArchiveMapViewModel(visits(row("first"), row("second")), FakeReviewChangeStore(), ReviewChangeReducer(), reviews)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("first"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertTrue(viewModel.uiState.value.isDeletingReview)
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("second"))
            viewModel.dispatch(ArchiveMapIntent.DismissReviewDeletion)
            runCurrent()
            assertEquals(listOf("first"), reviews.deletedReviewIds)
            assertEquals("first", viewModel.uiState.value.deleteTargetReviewId)
            pending.complete(RamapResult.Success(Unit))
            runCurrent()
            assertFalse(viewModel.uiState.value.isDeletingReview)
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
        }

    @Test
    fun `삭제 실패는 확인창과 기록을 유지하고 재시도로 복구한다`() =
        coroutinesTest {
            val row = row("review")
            val changes = FakeReviewChangeStore()
            val reviews = FakeArchiveReviewRepository().apply { delete = { RamapResult.Error(RamapError.Http(500)) } }
            val viewModel = ArchiveMapViewModel(visits(row), changes, ReviewChangeReducer(), reviews)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("review"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertEquals("review", viewModel.uiState.value.deleteTargetReviewId)
            assertEquals(1, viewModel.uiState.value.selectedReviews.size)
            assertFalse(viewModel.uiState.value.isDeletingReview)
            assertIs<ArchiveMapSideEffect.ShowToast>(viewModel.sideEffect.first())
            reviews.delete = {
                changes.publish(deleted(row))
                RamapResult.Success(Unit)
            }
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            assertEquals(listOf("review", "review"), reviews.deletedReviewIds)
        }

    @Test
    fun `계정 전환은 삭제 상태를 비우고 이전 요청의 늦은 응답을 무시한다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<Unit>>()
            val reviews = FakeArchiveReviewRepository().apply { delete = { withContext(NonCancellable) { pending.await() } } }
            val changes = FakeReviewChangeStore()
            val viewModel = ArchiveMapViewModel(visits(row("review")), changes, ReviewChangeReducer(), reviews)
            viewModel.dispatch(ArchiveMapIntent.Enter)
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.SelectShop("shop"))
            runCurrent()
            viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion("review"))
            viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion)
            runCurrent()
            assertTrue(viewModel.uiState.value.isDeletingReview)
            assertEquals(listOf("review"), reviews.deletedReviewIds)
            changes.reset(null)
            runCurrent()
            assertFalse(viewModel.uiState.value.isDeletingReview)
            assertNull(viewModel.uiState.value.deleteTargetReviewId)
            pending.complete(RamapResult.Success(Unit))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.shops
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.selectedShopId)
            assertFalse(viewModel.uiState.value.isDeletingReview)
        }

    private fun row(id: String) = MyReview(Review(id, "shop", "테스트 방문 후기", "2026-10-27", isPublic = false), shopName = "매장")

    private fun visits(vararg rows: MyReview) = FakeArchiveRepository(ArchiveShops(rows.toList(), RamenShops(listOf(ramenShopFixture(id = "shop")))))

    private fun deleted(row: MyReview) =
        ReviewChange.Written(
            reviewId = row.review.id,
            userId = "owner",
            shopId = "shop",
            before = ReviewSnapshot(row.review, row.shopName),
            after = null,
            counts = ReviewOwnerCounts(0, 0, 0, 0, false),
        )
}
