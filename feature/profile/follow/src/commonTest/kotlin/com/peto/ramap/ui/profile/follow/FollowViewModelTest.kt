package com.peto.ramap.ui.profile.follow

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.FollowAction
import com.peto.ramap.domain.model.community.FollowCounts
import com.peto.ramap.domain.model.community.FollowList
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakeProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FollowViewModelTest {
    @Test
    fun requestsTabAppearsOnlyAfterPositiveCountsArrive() =
        coroutinesTest {
            val repository = FakeFollowRepository()
            val pending = CompletableDeferred<RamapResult<FollowCounts>>()
            repository.countsPending = pending
            val viewModel = FollowViewModel(repository, FakeProfileRepository())
            runCurrent()
            assertEquals(listOf(FollowList.FOLLOWING, FollowList.FOLLOWERS), viewModel.uiState.value.availableLists)
            pending.complete(RamapResult.Success(FollowCounts(3L, 5L, 2L)))
            runCurrent()
            assertTrue(FollowList.REQUESTS in viewModel.uiState.value.availableLists)
            assertEquals(FollowList.FOLLOWING, viewModel.uiState.value.selectedList)
        }

    @Test
    fun approvingLastRequestHidesTabAndOpensFollowing() =
        coroutinesTest {
            val repository =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(3L, 1L, 1L))
                    connections =
                        mapOf(
                            FollowList.REQUESTS to listOf(PublicProfile("request", "요청사용자")),
                            FollowList.FOLLOWING to listOf(PublicProfile("following", "팔로잉사용자")),
                        )
                }
            val viewModel = FollowViewModel(repository, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(FollowIntent.Select(FollowList.REQUESTS))
            runCurrent()
            viewModel.dispatch(FollowIntent.Change("request", FollowAction.APPROVE))
            runCurrent()
            assertEquals(FollowList.FOLLOWING, viewModel.uiState.value.selectedList)
            assertEquals(
                4L,
                viewModel.uiState.value.counts
                    ?.followers,
            )
            assertFalse(FollowList.REQUESTS in viewModel.uiState.value.availableLists)
            assertEquals(
                listOf("following"),
                viewModel.uiState.value.profiles
                    .map { it.userId },
            )
        }

    @Test
    fun processingLoadedRequestsKeepsTabWhenAnotherPageRemains() =
        coroutinesTest {
            val requests = (0..20).map { PublicProfile("request$it", "요청사용자$it") }
            val repository =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(0L, 0L, 21L))
                    connections = mapOf(FollowList.REQUESTS to requests)
                }
            val viewModel = FollowViewModel(repository, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(FollowIntent.Select(FollowList.REQUESTS))
            runCurrent()
            for (request in requests.take(20)) {
                viewModel.dispatch(FollowIntent.Change(request.userId, FollowAction.REJECT))
                runCurrent()
            }
            assertEquals(FollowList.REQUESTS, viewModel.uiState.value.selectedList)
            assertEquals(
                1L,
                viewModel.uiState.value.counts
                    ?.requests,
            )
            assertEquals(
                listOf("request20"),
                viewModel.uiState.value.profiles
                    .map { it.userId },
            )
            assertTrue(FollowList.REQUESTS in viewModel.uiState.value.availableLists)
            assertEquals(FollowList.REQUESTS to 0L, repository.calls.last())
        }

    @Test
    fun failedRequestActionPreservesRowAndCounts() =
        coroutinesTest {
            val repository =
                FakeFollowRepository().apply {
                    countsResult = RamapResult.Success(FollowCounts(0L, 0L, 1L))
                    connections = mapOf(FollowList.REQUESTS to listOf(PublicProfile("request", "요청사용자")))
                    changeResult = RamapResult.Error(RamapError.Http(500))
                }
            val viewModel = FollowViewModel(repository, FakeProfileRepository())
            runCurrent()
            viewModel.dispatch(FollowIntent.Select(FollowList.REQUESTS))
            runCurrent()
            viewModel.dispatch(FollowIntent.Change("request", FollowAction.REJECT))
            runCurrent()
            assertEquals(FollowList.REQUESTS, viewModel.uiState.value.selectedList)
            assertEquals(
                1L,
                viewModel.uiState.value.counts
                    ?.requests,
            )
            assertEquals(
                "request",
                viewModel.uiState.value.profiles
                    .single()
                    .userId,
            )
            assertFalse(viewModel.uiState.value.mutating)
        }

    @Test
    fun cancelledCountsCannotRestoreTabsAfterLogout() =
        coroutinesTest {
            val repository = FakeFollowRepository()
            val pending = CompletableDeferred<RamapResult<FollowCounts>>()
            repository.countsPending = pending
            val profiles = FakeProfileRepository()
            val viewModel = FollowViewModel(repository, profiles)
            runCurrent()
            profiles.sessionUserIds.value = null
            runCurrent()
            pending.complete(RamapResult.Success(FollowCounts(3L, 5L, 2L)))
            runCurrent()
            assertEquals(null, viewModel.uiState.value.currentUserId)
            assertEquals(null, viewModel.uiState.value.counts)
            assertFalse(FollowList.REQUESTS in viewModel.uiState.value.availableLists)
        }
}
