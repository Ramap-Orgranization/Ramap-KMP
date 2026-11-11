package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.follow.FollowCounts
import com.peto.ramap.fake.FakeAnnouncementReadStore
import com.peto.ramap.fake.FakeAnnouncementRepository
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeProfileRepository
import com.peto.ramap.fake.FakeProfileVisitRankingRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabFollowBadgeTest {
    @Test
    fun `신청 없이 새 팔로워만 있어도 N을 표시한다`() =
        coroutinesTest {
            val follows =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(1L, 0L, 0L, hasUnreadFollowers = true))
                }
            val viewModel = viewModel(follows)
            runCurrent()

            assertTrue(viewModel.uiState.value.hasUnreadFollowConnections)
            assertFalse(viewModel.uiState.value.hasPendingFollowRequests)
        }

    @Test
    fun `신청을 확인하고 돌아오면 N만 사라지고 대기 중 신청 표시는 유지한다`() =
        coroutinesTest {
            val follows =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(0L, 0L, 1L, hasUnreadRequests = true))
                }
            val viewModel = viewModel(follows)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadFollowConnections)

            follows.countsResult = RamapResult.Success(FollowCounts(0L, 0L, 1L))
            viewModel.dispatch(MyTabIntent.ReturnedFromFollows)
            runCurrent()

            assertFalse(viewModel.uiState.value.hasUnreadFollowConnections)
            assertTrue(viewModel.uiState.value.hasPendingFollowRequests)

            follows.countsResult = RamapResult.Success(FollowCounts(0L, 0L, 1L, hasUnreadRequests = true))
            viewModel.dispatch(MyTabIntent.ReturnedToScreen)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadFollowConnections)
        }

    @Test
    fun `갱신 실패로 확인하지 않은 팔로우 표시를 지우지 않는다`() =
        coroutinesTest {
            val follows =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(1L, 0L, 0L, hasUnreadFollowers = true))
                }
            val viewModel = viewModel(follows)
            runCurrent()
            follows.countsResult = RamapResult.Error(RamapError.Http(500))
            viewModel.dispatch(MyTabIntent.ReturnedToScreen)
            runCurrent()

            assertTrue(viewModel.uiState.value.hasUnreadFollowConnections)
        }

    @Test
    fun `계정 전환 뒤 이전 계정 응답으로 N을 복원하지 않는다`() =
        coroutinesTest {
            val pending = CompletableDeferred<RamapResult<FollowCounts>>()
            val follows = FakeFollowRepository().apply { countsPending = pending }
            val profiles = FakeProfileRepository()
            val viewModel = viewModel(follows, profiles)
            runCurrent()
            follows.countsPending = null
            profiles.sessionUserId.value = "second-account"
            runCurrent()
            pending.complete(RamapResult.Success(FollowCounts(1L, 0L, 0L, hasUnreadFollowers = true)))
            runCurrent()

            assertFalse(viewModel.uiState.value.hasUnreadFollowConnections)
        }

    private fun viewModel(
        follows: FakeFollowRepository,
        profiles: FakeProfileRepository = FakeProfileRepository(),
    ): MyTabViewModel =
        MyTabViewModel(
            profiles,
            FakeUserModerationRepository(),
            FakePersonalizationRepository(),
            FakeMyTabReviewRepository(),
            follows,
            FakeReviewChangeStore(),
            FakeAnnouncementRepository(),
            FakeAnnouncementReadStore(),
            FakeProfileVisitRankingRepository(),
        )
}
