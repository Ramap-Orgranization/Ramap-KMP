package com.peto.ramap.ui.review.other

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.ProfileReview
import com.peto.ramap.domain.model.community.ReviewAuthor
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.change.ReviewChangeReducer
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.review.other.contract.OtherReviewsEffect
import com.peto.ramap.ui.review.other.contract.OtherReviewsIntent
import com.peto.ramap.ui.review.other.contract.OtherReviewsTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.follow_update_failure_message
import ramap.shared.generated.resources.login_required_block_description
import ramap.shared.generated.resources.login_required_saved_shops_description
import ramap.shared.generated.resources.review_block_failed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OtherReviewsViewModelTest {
    @Test
    fun `본인 프로필은 비공개 미게시 리뷰의 등록과 수정과 삭제 및 집계를 재조회 없이 반영한다`() =
        coroutinesTest {
            val changes = FakeReviewChangeStore()
            val community =
                FakeOtherReviewsCommunity().apply {
                    profile =
                        com.peto.ramap.domain.model.community
                            .PublicProfile("owner", "작성자")
                    reviewCount = 0
                }
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository("owner"),
                    follows = FakeFollowRepository(),
                    changes = changes,
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("owner"))
            runCurrent()
            val original = Review("private", "shop", "비공개 본문", "2026-10-12", author = ReviewAuthor("owner", "작성자"), isPublic = false, moderationStatus = com.peto.ramap.domain.model.community.ReviewModerationStatus.PENDING)

            fun write(
                before: Review?,
                after: Review?,
                total: Int,
            ) = com.peto.ramap.domain.model.review.change.ReviewChange
                .Written(
                    "private",
                    "owner",
                    "shop",
                    before?.let {
                        com.peto.ramap.domain.model.review.change
                            .ReviewSnapshot(it, "매장")
                    },
                    after?.let {
                        com.peto.ramap.domain.model.review.change
                            .ReviewSnapshot(it, "매장")
                    },
                    com.peto.ramap.domain.model.review.change
                        .ReviewOwnerCounts(total, 0, total, 0, false),
                )
            changes.publish(write(null, original, 1))
            runCurrent()
            assertEquals(
                listOf("private"),
                viewModel.uiState.value.reviews
                    .map { it.review.id },
            )
            assertEquals(1L, (viewModel.uiState.value.profileAccess as com.peto.ramap.domain.model.community.ProfileAccess.Visible).reviewCount)
            val edited = original.copy(body = "수정된 비공개 본문")
            changes.publish(write(original, edited, 1))
            runCurrent()
            assertEquals(
                "수정된 비공개 본문",
                viewModel.uiState.value.reviews
                    .single()
                    .review.body,
            )
            changes.publish(write(edited, null, 0))
            runCurrent()
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertEquals(0L, (viewModel.uiState.value.profileAccess as com.peto.ramap.domain.model.community.ProfileAccess.Visible).reviewCount)
            assertEquals(listOf(0L), community.requestedOffsets)
        }

    @Test
    fun `비로그인 사용자의 차단 요청은 작성자를 차단하지 않고 로그인을 안내한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(userId = null),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
                assertEquals(OtherReviewsEffect.LoginRequired(Res.string.login_required_block_description), awaitItem())
            }
            assertTrue(community.blockedUserIds.isEmpty())
        }

    @Test
    fun `프로필 리뷰는 페이지 단위로 조회하며 마지막 페이지에서 멈춘다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = (1..23).map(::review)
            community.reviewCount = 23L
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            assertEquals(
                "라멘팬",
                viewModel.uiState.value.profile
                    ?.nickname,
            )
            assertEquals(20, viewModel.uiState.value.reviews.size)
            assertTrue(viewModel.uiState.value.hasMore)

            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()
            assertEquals(listOf("author"), community.requestedProfileUserIds)
            assertEquals(listOf(0L, 20L), community.requestedOffsets)
            assertEquals(community.reviews, viewModel.uiState.value.reviews)
            assertEquals(23, viewModel.uiState.value.reviews.size)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `페이지 사이의 중복 리뷰는 기존 매장을 유지하며 조회 위치를 증가시킨다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val firstPage = (1..20).map(::review)
            community.reviews = firstPage + firstPage.first().copy(shop = ramenShopFixture(id = "shop-1"))
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()

            assertEquals(firstPage, viewModel.uiState.value.reviews)
            assertEquals(21L, viewModel.uiState.value.reviewsOffset)
            assertEquals(listOf(0L, 20L), community.requestedOffsets)
            assertFalse(viewModel.uiState.value.hasMore)
        }

    @Test
    fun `저장한 매장 탭은 공개 매장을 페이지 단위로 조회한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.savedShops = (1..23).map { ramenShopFixture(id = "shop-$it") }
            community.savedShopCount = 23L
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.SelectTab(OtherReviewsTab.SavedShops))
            runCurrent()

            assertEquals(OtherReviewsTab.SavedShops, viewModel.uiState.value.selectedTab)
            assertEquals(20, viewModel.uiState.value.savedShops.size)
            assertTrue(viewModel.uiState.value.savedShopsHasMore)

            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()

            assertEquals(listOf(0L, 20L), community.requestedSavedShopOffsets)
            assertEquals(23, viewModel.uiState.value.savedShops.size)
            assertFalse(viewModel.uiState.value.savedShopsHasMore)
        }

    @Test
    fun `비로그인 사용자의 저장한 매장 탭은 매장을 조회하지 않고 로그인을 안내한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(userId = null),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.SelectTab(OtherReviewsTab.SavedShops))
                assertEquals(OtherReviewsEffect.LoginRequired(Res.string.login_required_saved_shops_description), awaitItem())
            }

            assertEquals(OtherReviewsTab.Reviews, viewModel.uiState.value.selectedTab)
            assertTrue(community.requestedSavedShopOffsets.isEmpty())
        }

    @Test
    fun `이용할 수 없는 프로필은 리뷰를 조회하지 않는다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.profile = null
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.loaded)
            assertEquals(null, viewModel.uiState.value.profile)
            assertFalse(viewModel.uiState.value.isPrivate)
            assertTrue(community.requestedOffsets.isEmpty())
        }

    @Test
    fun `비공개 프로필은 사용자 정보와 리뷰를 숨긴다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.profilePrivate = true
            community.reviews = listOf(review(1))
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.loaded)
            assertTrue(viewModel.uiState.value.isPrivate)
            assertEquals(null, viewModel.uiState.value.profile)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertTrue(community.requestedOffsets.isEmpty())
            assertTrue(community.requestedSavedShopOffsets.isEmpty())
        }

    @Test
    fun `차단한 프로필은 사용자 정보를 유지하고 리뷰를 숨긴다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.blockedUserIds += "author"
            community.reviews = listOf(review(1))
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            assertTrue(viewModel.uiState.value.isBlocked)
            assertEquals(listOf("author"), community.requestedProfileUserIds)
            assertFalse(viewModel.uiState.value.isPrivate)
            assertEquals(
                "라멘팬",
                viewModel.uiState.value.profile
                    ?.nickname,
            )
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertTrue(community.requestedOffsets.isEmpty())
        }

    @Test
    fun `다음 페이지 조회 실패 시 같은 위치를 재시도한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = (1..23).map(::review)
            community.reviewCount = 23L
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            community.failNextPage = true
            viewModel.dispatch(OtherReviewsIntent.LoadMore)
            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertEquals(20, viewModel.uiState.value.reviews.size)

            viewModel.dispatch(OtherReviewsIntent.Retry)
            runCurrent()
            assertEquals(listOf(0L, 20L, 20L), community.requestedOffsets)
            assertEquals(23, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `차단하면 프로필 리뷰를 숨기고 차단을 해제하면 복원한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = listOf(review(1))
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
            runCurrent()
            assertTrue(viewModel.uiState.value.isBlocked)
            assertTrue(
                viewModel.uiState.value.reviews
                    .isEmpty(),
            )
            assertEquals(setOf("author"), community.blockedUserIds)

            viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
            runCurrent()
            assertFalse(viewModel.uiState.value.isBlocked)
            assertEquals(1, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `차단 실패 시 리뷰를 계속 표시한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            community.reviews = listOf(review(1))
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = FakeFollowRepository(),
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            runCurrent()
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            community.failNextBlock = true
            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.ToggleBlock)
                runCurrent()
                val effect = awaitItem()
                assertTrue(effect is OtherReviewsEffect.ShowToast)
                assertEquals(Res.string.review_block_failed, effect.data.message)
            }
            assertFalse(viewModel.uiState.value.isBlocked)
            assertEquals(1, viewModel.uiState.value.reviews.size)
        }

    @Test
    fun `팔로우 변경 실패는 조회나 차단 실패와 구분해 안내한다`() =
        coroutinesTest {
            val community = FakeOtherReviewsCommunity()
            val follows =
                FakeFollowRepository().apply {
                    changeResult = RamapResult.Error(RamapError.Http(500))
                }
            val viewModel =
                OtherReviewsViewModel(
                    community = community,
                    profiles = FakeProfileRepository(),
                    follows = follows,
                    changes = FakeReviewChangeStore(),
                    reviewChangeReducer = ReviewChangeReducer(),
                )
            viewModel.dispatch(OtherReviewsIntent.OpenProfile("author"))
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(OtherReviewsIntent.ToggleFollow)
                runCurrent()
                val effect = awaitItem()
                assertTrue(effect is OtherReviewsEffect.ShowToast)
                assertEquals(Res.string.follow_update_failure_message, effect.data.message)
                assertFalse(viewModel.uiState.value.following)
                assertFalse(viewModel.uiState.value.isBlocked)
            }
        }

    private fun review(number: Int) =
        ProfileReview(
            review =
                Review(
                    id = "review-$number",
                    shopId = "shop-$number",
                    body = "맛있는 라멘이에요",
                    createdAt = "2026-09-30T12:00:00Z",
                    author = ReviewAuthor("author", "라멘팬"),
                ),
            shop = if (number % 2 == 0) ramenShopFixture(id = "shop-$number") else null,
        )
}
