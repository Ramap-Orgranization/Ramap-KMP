package com.peto.ramap.ui.review.write

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.designsystem.toast.model.ToastType
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.domain.model.review.EditableReview
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewImage
import com.peto.ramap.domain.repository.ProfileRepository
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.write.contract.ReviewWriteIntent
import com.peto.ramap.ui.review.write.contract.ReviewWriteSideEffect
import kotlinx.coroutines.CompletableDeferred
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
    fun `비속어 리뷰의 신규 등록과 수정을 막고 정상 내용으로 수정하면 저장한다`() =
        coroutinesTest {
            for (reviewId in listOf(null, "review")) {
                val reviews =
                    FakeReviewRepository().apply {
                        editableReview = EditableReview("review", "shop", "기존 리뷰입니다", emptyList(), emptyList(), false)
                    }
                val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
                viewModel.dispatch(ReviewWriteIntent.Open("shop", reviewId))
                runCurrent()
                viewModel.dispatch(ReviewWriteIntent.ChangeBody("씨.발 맛없어요"))
                viewModel.dispatch(ReviewWriteIntent.Submit)
                runCurrent()
                assertTrue(viewModel.uiState.value.bodyContainsProfanity)
                assertFalse(viewModel.uiState.value.canSubmit)
                assertTrue(reviews.submissions.isEmpty())
                assertTrue(reviews.updates.isEmpty())

                viewModel.dispatch(ReviewWriteIntent.ChangeBody("국물이 진하고 맛있어요"))
                runCurrent()
                assertFalse(viewModel.uiState.value.bodyContainsProfanity)
                assertTrue(viewModel.uiState.value.canSubmit)
                viewModel.dispatch(ReviewWriteIntent.Submit)
                runCurrent()
                assertEquals(1, reviews.submissions.size + reviews.updates.size)
            }
        }

    @Test
    fun `reopening the same review reloads the saved body and photos`() =
        coroutinesTest {
            val reviews =
                FakeReviewRepository().apply {
                    editableReview = EditableReview("review", "shop", "이전 내용", listOf("old"), listOf("old-url"), true)
                }
            val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop", "review"))
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.ChangeBody("수정한 내용"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()

            reviews.editableReview = EditableReview("review", "shop", "수정한 내용", listOf("new"), listOf("new-url"), true)
            viewModel.dispatch(ReviewWriteIntent.Open("shop", "review"))
            runCurrent()

            assertEquals("수정한 내용", viewModel.uiState.value.body)
            assertEquals(
                listOf("new"),
                viewModel.uiState.value.existingImages
                    .map { it.path },
            )
        }

    @Test
    fun `pending profile visibility update cannot submit a draft to another shop`() =
        coroutinesTest {
            val pendingUpdate = CompletableDeferred<RamapResult<AccountProfile>>()
            val baseProfiles = FakeProfileRepository(isProfilePublic = false)
            val profiles =
                object : ProfileRepository by baseProfiles {
                    override suspend fun updateProfileVisibility(isPublic: Boolean): RamapResult<AccountProfile> = pendingUpdate.await()
                }
            val reviews = FakeReviewRepository()
            val viewModel = ReviewWriteViewModel(reviews, profiles, FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("first"))
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.ChangeBody("first draft"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.ConfirmSubmitWithPublicProfile)
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.Open("second"))
            runCurrent()
            pendingUpdate.complete(RamapResult.Success(AccountProfile("me", "라멘러", isPublic = true)))
            runCurrent()

            assertEquals(emptyList(), reviews.submissions)
            assertEquals("", viewModel.uiState.value.body)
        }

    @Test
    fun `edit loads owner draft and saves retained photos privacy and new photo`() =
        coroutinesTest {
            val reviews =
                FakeReviewRepository().apply {
                    editableReview =
                        EditableReview(
                            id = "review",
                            shopId = "shop",
                            body = "기존 리뷰입니다",
                            imagePaths = listOf("first", "second"),
                            imageUrls = listOf(null, "signed-second"),
                            isPublic = false,
                        )
                }
            val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop", "review"))
            runCurrent()
            assertEquals("기존 리뷰입니다", viewModel.uiState.value.body)
            assertEquals(
                listOf("first", "second"),
                viewModel.uiState.value.existingImages
                    .map { it.path },
            )
            assertFalse(viewModel.uiState.value.isPublic)

            viewModel.dispatch(ReviewWriteIntent.RemoveExistingImage(0))
            viewModel.dispatch(ReviewWriteIntent.AddImage(reviewImage(1)))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()
            assertEquals(listOf("second"), reviews.updates.single().retainedImagePaths)
            assertEquals(
                1,
                reviews.updates
                    .single()
                    .newImages.size,
            )
            assertFalse(reviews.updates.single().isPublic)
        }

    @Test
    fun `failed edit retains body and owner photo paths for retry`() =
        coroutinesTest {
            val reviews =
                FakeReviewRepository().apply {
                    editableReview = EditableReview("review", "shop", "기존 리뷰입니다", listOf("path"), listOf(null), true)
                    editResult = RamapResult.Error(RamapError.Network(IllegalStateException("offline")))
                }
            val viewModel = ReviewWriteViewModel(reviews, FakeProfileRepository(), FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop", "review"))
            runCurrent()
            viewModel.dispatch(ReviewWriteIntent.ChangeBody("수정된 리뷰입니다"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()
            assertEquals("수정된 리뷰입니다", viewModel.uiState.value.body)
            assertEquals(
                listOf("path"),
                viewModel.uiState.value.existingImages
                    .map { it.path },
            )
            assertEquals(listOf("path"), reviews.updates.single().retainedImagePaths)
        }

    @Test
    fun `account switch clears edit draft and reloads owner data`() =
        coroutinesTest {
            val profiles = FakeProfileRepository("first")
            val reviews =
                FakeReviewRepository().apply {
                    editableReview = EditableReview("review", "shop", "첫 계정 리뷰", listOf("first"), listOf("url"), true)
                }
            val viewModel = ReviewWriteViewModel(reviews, profiles, FakeRamenShopRepository())
            viewModel.dispatch(ReviewWriteIntent.Open("shop", "review"))
            runCurrent()
            assertEquals("첫 계정 리뷰", viewModel.uiState.value.body)

            reviews.editableReview = EditableReview("review", "shop", "새 계정 리뷰", listOf("second"), listOf(null), false)
            profiles.sessionUserIds.value = "second"
            runCurrent()
            assertEquals("새 계정 리뷰", viewModel.uiState.value.body)
            assertEquals(
                listOf("second"),
                viewModel.uiState.value.existingImages
                    .map { it.path },
            )
        }

    @Test
    fun `opening another shop clears draft and submits only to the new shop`() =
        coroutinesTest {
            val reviews = FakeReviewRepository()
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
            val reviews = FakeReviewRepository()
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
                FakeReviewRepository(
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
            val viewModel = ReviewWriteViewModel(FakeReviewRepository(), FakeProfileRepository(null), FakeRamenShopRepository())
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
                    FakeReviewRepository(),
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
            val reviews = FakeReviewRepository()
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
            val reviews = FakeReviewRepository()
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
                    FakeReviewRepository(),
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
                    FakeReviewRepository(),
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
                    FakeReviewRepository(),
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
                    FakeReviewRepository(),
                    FakeProfileRepository(),
                    ramenShops,
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            assertEquals(listOf("shop"), ramenShops.requestedShopDetailIds)
            assertEquals(shop, viewModel.uiState.value.shop)
        }

    @Test
    fun `submitting public review with private profile shows modal and selecting public updates profile and submits`() =
        coroutinesTest {
            val reviews = FakeReviewRepository()
            val profileRepository = FakeProfileRepository(isProfilePublic = false)
            val viewModel =
                ReviewWriteViewModel(
                    reviews,
                    profileRepository,
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()

            assertTrue(viewModel.uiState.value.showPrivateProfileConfirmation)
            assertEquals(0, reviews.submissions.size)

            viewModel.dispatch(ReviewWriteIntent.ConfirmSubmitWithPublicProfile)
            runCurrent()

            assertTrue(profileRepository.isProfilePublic)
            assertEquals(1, reviews.submissions.size)
            assertFalse(viewModel.uiState.value.showPrivateProfileConfirmation)
        }

    @Test
    fun `submitting public review with private profile shows modal and selecting private submits without updating profile`() =
        coroutinesTest {
            val reviews = FakeReviewRepository()
            val profileRepository = FakeProfileRepository(isProfilePublic = false)
            val viewModel =
                ReviewWriteViewModel(
                    reviews,
                    profileRepository,
                    FakeRamenShopRepository(),
                )
            viewModel.dispatch(ReviewWriteIntent.Open("shop"))
            runCurrent()

            viewModel.dispatch(ReviewWriteIntent.ChangeBody("12345"))
            viewModel.dispatch(ReviewWriteIntent.Submit)
            runCurrent()

            assertTrue(viewModel.uiState.value.showPrivateProfileConfirmation)

            viewModel.dispatch(ReviewWriteIntent.ConfirmSubmitWithPrivateProfile)
            runCurrent()

            assertFalse(profileRepository.isProfilePublic)
            assertEquals(1, reviews.submissions.size)
            assertFalse(viewModel.uiState.value.showPrivateProfileConfirmation)
        }

    private fun reviewImage(seed: Int): ReviewImage =
        ReviewImage(
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), seed.toByte()),
            mimeType = ReviewImage.JPEG_MIME_TYPE,
        )
}
