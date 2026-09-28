package com.peto.ramap.ui.review.write

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.review_load_failed
import ramap.shared.generated.resources.shop_review_failure_message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewWriteViewModelTest {
    @Test
    fun `opening another shop clears draft and submits only to the new shop`() =
        coroutinesTest {
            val reviews = FakeShopReviewRepository()
            val ramenShops = FakeRamenShopRepository()
            val viewModel =
                ReviewWriteViewModel(
                    reviews,
                    FakeProfileRepository(),
                    ramenShops,
                )
            viewModel.dispatch(ReviewWriteIntent.Open("first"))
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.ChangeBody("first draft"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.Open("second"))
            runCurrent()
            assertEquals("", viewModel.uiState.value.body)
            assertEquals(listOf("first", "second"), ramenShops.requestedShopDetailIds)

            viewModel.dispatch(ReviewWriteIntent.ChangeBody("second draft"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()
            assertEquals("second", reviews.submissions.single().shopId)
        }

    @Test
    fun `submission clears the draft`() =
        coroutinesTest {
            val reviews = FakeShopReviewRepository()
            val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.sideEffect.test {
                assertEquals(
                    ReviewWriteSideEffect.ShowToast(
                        ToastData(Res.string.review_load_failed, ToastType.ERROR),
                        canRetry = true,
                    ),
                    awaitItem(),
                )
                viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
                viewModel.dispatch(ReviewWriteIntent.Submit)
                assertEquals(ReviewWriteSideEffect.Submitted, awaitItem())
            }

            assertEquals("shop", reviews.submissions.single().shopId)
            assertEquals("12345", reviews.submissions.single().body)
            assertEquals(emptyList<ReviewImage>(), reviews.submissions.single().images)
            assertTrue(reviews.submissions.single().isPublic)
            assertEquals("", viewModel.uiState.value.body)
        }

    @Test
    fun `submission failure keeps the draft`() =
        coroutinesTest {
            val reviews =
                FakeShopReviewRepository(
                    submitResult = RamapResult.Error(RamapError.Network(IllegalStateException("offline"))),
                )
            val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.sideEffect.test {
                assertEquals(
                    ReviewWriteSideEffect.ShowToast(
                        ToastData(Res.string.review_load_failed, ToastType.ERROR),
                        canRetry = true,
                    ),
                    awaitItem(),
                )
                viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
                viewModel.dispatch(ReviewWriteIntent.Submit)
                assertEquals(
                    ReviewWriteSideEffect.ShowToast(
                        ToastData(Res.string.shop_review_failure_message, ToastType.ERROR),
                    ),
                    awaitItem(),
                )
            }

            assertEquals(1, reviews.submissions.size)
            assertEquals("12345", viewModel.uiState.value.body)
            assertFalse(viewModel.uiState.value.isSubmitting)
        }

    @Test
    fun `submission without a session requests login`() =
        coroutinesTest {
            val viewModel = ReviewWriteViewModel(FakeShopReviewRepository(), FakeProfileRepository(null), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.sideEffect.test {
                assertEquals(
                    ReviewWriteSideEffect.ShowToast(
                        ToastData(Res.string.review_load_failed, ToastType.ERROR),
                        canRetry = true,
                    ),
                    awaitItem(),
                )
                viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
                viewModel.dispatch(ReviewWriteIntent.Submit)
                assertEquals(ReviewWriteSideEffect.LoginRequired, awaitItem())
            }
        }

    @Test
    fun `review body is limited to thirty Unicode characters`() =
        coroutinesTest {
            val viewModel =
                ReviewWriteViewModel(
                    FakeShopReviewRepository(),
                    FakeProfileRepository(),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.ChangeBody("😀".repeat(31)))
            runCurrent()

            assertEquals(30, Review.bodyCharacterCount(viewModel.uiState.value.body))
        }

    @Test
    fun `moving images changes representative image and keeps the submitted order`() =
        coroutinesTest {
            val first = reviewImage(1)
            val second = reviewImage(2)
            val third = reviewImage(3)
            val reviews = FakeShopReviewRepository()
            val viewModel =
                ReviewWriteViewModel(
                    reviews,
                    FakeProfileRepository(),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.AddImage(first))
            viewModel.dispatch(ReviewWriteIntent.AddImage(second))
            viewModel.dispatch(ReviewWriteIntent.AddImage(third))
            viewModel.dispatch(ReviewWriteIntent.MoveImage(2, 0))
            viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()

            assertEquals(listOf(third, first, second), reviews.submissions.single().images)
        }

    @Test
    fun `private visibility is passed to the repository and restored after submission`() =
        coroutinesTest {
            val reviews = FakeShopReviewRepository()
            val viewModel =
                ReviewWriteViewModel(
                    reviews,
                    FakeProfileRepository(),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
            viewModel.dispatch(ReviewWriteIntent.ChangeVisibility(false))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()

            assertFalse(reviews.submissions.single().isPublic)
            assertTrue(viewModel.uiState.value.isPublic)
        }

    @Test
    fun `guest can choose review visibility before login`() =
        coroutinesTest {
            val viewModel =
                ReviewWriteViewModel(
                    FakeShopReviewRepository(),
                    FakeProfileRepository(null),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.ChangeVisibility(false))
            runCurrent()

            assertFalse(viewModel.uiState.value.isPublic)
        }

    @Test
    fun `adding a fourth image is ignored`() =
        coroutinesTest {
            val viewModel =
                ReviewWriteViewModel(
                    FakeShopReviewRepository(),
                    FakeProfileRepository(),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            (1..4).forEach { viewModel.dispatch(ReviewWriteIntent.AddImage(reviewImage(it))) }
            runCurrent()

            assertEquals(3, viewModel.uiState.value.images.size)
        }

    @Test
    fun `moving an image with invalid indexes is ignored`() =
        coroutinesTest {
            val first = reviewImage(1)
            val second = reviewImage(2)
            val viewModel =
                ReviewWriteViewModel(
                    FakeShopReviewRepository(),
                    FakeProfileRepository(),
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.AddImage(first))
            viewModel.dispatch(ReviewWriteIntent.AddImage(second))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.MoveImage(-1, 0))
            viewModel.dispatch(ReviewWriteIntent.MoveImage(0, 2))
            runCurrent()

            assertEquals(listOf(first, second), viewModel.uiState.value.images)
        }

    @Test
    fun `loads the shop used by the review identity card`() =
        coroutinesTest {
            val shop = ramenShopFixture(id = "shop")
            val ramenShops = FakeRamenShopRepository(shopDetail = ShopDetail(shop, 0, null, null, null))
            val viewModel =
                ReviewWriteViewModel(
                    FakeShopReviewRepository(),
                    FakeProfileRepository(),
                    ramenShops,
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            assertEquals(listOf("shop"), ramenShops.requestedShopDetailIds)
            assertEquals(shop, viewModel.uiState.value.shop)
        }

    private fun reviewImage(seed: Int): ReviewImage =
        ReviewImage(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), seed.toByte()),
            mimeType = ReviewImage.JPEG_MIME_TYPE,
        )
}
