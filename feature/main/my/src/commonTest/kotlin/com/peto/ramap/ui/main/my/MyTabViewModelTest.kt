package com.peto.ramap.ui.main.my

import app.cash.turbine.test
import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.community.PublicProfile
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeReviewCommunityRepository
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.contract.MyTabSideEffect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.profile_save_failed
import ramap.shared.generated.resources.review_blocked_users_empty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabViewModelTest {
    @Test
    fun `프로필 변경을 상태에 반영하면서 추가 조회는 하지 않는다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()
            assertEquals(1, profiles.fetchCount)

            profiles.profileUpdates.emit(AccountProfile("first", "변경된 닉네임"))
            runCurrent()

            assertEquals(
                "변경된 닉네임",
                viewModel.uiState.value.profile
                    ?.nickname,
            )
            assertEquals(1, profiles.fetchCount)
        }

    @Test
    fun `내 리뷰와 차단한 사용자 수를 표시하고 갱신한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val reviews = FakeMyTabReviewRepository(totalCount = 7)
            val community =
                FakeReviewCommunityRepository(
                    blockedUsersResult = RamapResult.Success(listOf(PublicProfile("user1", "차단사용자1"))),
                )
            val viewModel = MyTabViewModel(profiles, community, FakePersonalizationRepository(), reviews)
            runCurrent()

            assertEquals(7, viewModel.uiState.value.reviewCount)
            assertEquals(1, viewModel.uiState.value.blockedUserCount)

            reviews.totalCount = 8
            community.blockedUsersResult = RamapResult.Success(emptyList())
            viewModel.dispatch(MyTabIntent.Refresh)
            runCurrent()

            assertEquals(8, viewModel.uiState.value.reviewCount)
            assertEquals(0, viewModel.uiState.value.blockedUserCount)

            reviews.totalCount = 9
            reviews.changes.emit(Unit)
            runCurrent()
            assertEquals(9, viewModel.uiState.value.reviewCount)

            profiles.sessionUserIds.value = null
            runCurrent()
            assertNull(viewModel.uiState.value.reviewCount)
            assertNull(viewModel.uiState.value.blockedUserCount)
        }

    @Test
    fun `첫 세션 응답 전에는 프로필 표시를 보류하고 조회 시작 시 로딩 상태가 된다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val pending = CompletableDeferred<RamapResult<AccountProfile>>()
            profiles.fetchPending = pending
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())

            assertFalse(viewModel.uiState.value.sessionResolved)
            assertNull(viewModel.uiState.value.profile)
            runCurrent()
            assertTrue(viewModel.uiState.value.sessionResolved)
            assertTrue(viewModel.uiState.value.loading)
            assertNull(viewModel.uiState.value.profile)
            pending.complete(RamapResult.Success(AccountProfile("first", "느긋한차슈")))
            runCurrent()
            assertFalse(viewModel.uiState.value.loading)
            val loadedProfile = viewModel.uiState.value.profile
            assertEquals("느긋한차슈", loadedProfile?.nickname)
        }

    @Test
    fun `프로필과 개인화 개수를 표시하고 세션이 바뀌면 프로필을 비운다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val personalization =
                FakePersonalizationRepository(
                    ShopPersonalization(
                        bookmarkedShopIds = setOf("one"),
                        notificationShopIds = setOf("two", "three"),
                        hiddenShopIds = setOf("four", "five", "six"),
                    ),
                )
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), personalization, FakeMyTabReviewRepository())
            assertFalse(viewModel.uiState.value.sessionResolved)
            runCurrent()

            val initialProfile = viewModel.uiState.value.profile
            assertTrue(viewModel.uiState.value.sessionResolved)
            assertEquals("느긋한차슈", initialProfile?.nickname)
            assertEquals(1, viewModel.uiState.value.bookmarkedCount)
            assertEquals(2, viewModel.uiState.value.notificationCount)
            assertEquals(3, viewModel.uiState.value.hiddenCount)
            personalization.updateBookmarkedShopIds(setOf("one", "two"))
            runCurrent()
            assertEquals(2, viewModel.uiState.value.bookmarkedCount)

            profiles.fetchResult = RamapResult.Success(AccountProfile("first", "새 닉네임"))
            viewModel.dispatch(MyTabIntent.Refresh)
            runCurrent()
            val updatedProfile = viewModel.uiState.value.profile
            assertEquals("새 닉네임", updatedProfile?.nickname)

            profiles.sessionUserIds.value = null
            runCurrent()
            assertNull(viewModel.uiState.value.profile)
            assertTrue(viewModel.uiState.value.sessionResolved)
        }

    @Test
    fun `이전 갱신 응답이 늦게 도착해도 최신 프로필을 유지한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            val oldRequest = CompletableDeferred<RamapResult<AccountProfile>>()
            profiles.fetchPending = oldRequest
            profiles.ignoreFetchCancellation = true
            viewModel.dispatch(MyTabIntent.Refresh)
            runCurrent()
            assertTrue(viewModel.uiState.value.loading)
            assertTrue(viewModel.uiState.value.sessionResolved)

            profiles.fetchPending = null
            profiles.fetchResult = RamapResult.Success(AccountProfile("first", "최신 닉네임"))
            viewModel.dispatch(MyTabIntent.Refresh)
            runCurrent()
            oldRequest.complete(RamapResult.Success(AccountProfile("first", "이전 닉네임")))
            runCurrent()

            val currentProfile = viewModel.uiState.value.profile
            assertEquals("최신 닉네임", currentProfile?.nickname)
            assertFalse(viewModel.uiState.value.loading)
        }

    @Test
    fun `로그아웃 후 도착한 프로필 응답을 무시한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            val oldRequest = CompletableDeferred<RamapResult<AccountProfile>>()
            profiles.fetchPending = oldRequest
            profiles.ignoreFetchCancellation = true
            viewModel.dispatch(MyTabIntent.Refresh)
            runCurrent()
            profiles.sessionUserIds.value = null
            runCurrent()
            oldRequest.complete(RamapResult.Success(AccountProfile("first", "이전 사용자")))
            runCurrent()

            assertNull(viewModel.uiState.value.profile)
            assertNull(viewModel.uiState.value.userId)
            assertFalse(viewModel.uiState.value.loading)
        }

    @Test
    fun `공개 설정 저장에 성공하면 프로필을 갱신한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.dispatch(MyTabIntent.SaveVisibility(true))
            runCurrent()

            assertTrue(
                viewModel.uiState.value.profile
                    ?.isPublic == true,
            )
        }

    @Test
    fun `공개 설정 저장 실패는 저장된 공개 상태를 유지하고 토스트를 표시한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            profiles.visibilityResult = RamapResult.Error(RamapError.Unknown(IllegalStateException("failed")))
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(MyTabIntent.SaveVisibility(true))
                runCurrent()

                val sideEffect = awaitItem()
                assertTrue(sideEffect is MyTabSideEffect.ShowToast)
                assertEquals(Res.string.profile_save_failed, sideEffect.data.message)
                assertFalse(
                    viewModel.uiState.value.profile
                        ?.isPublic == true,
                )
            }
        }

    @Test
    fun `같은 공개 상태 저장은 프로필 변경 요청을 하지 않는다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.dispatch(MyTabIntent.SaveVisibility(false))
            runCurrent()
            assertFalse(
                viewModel.uiState.value.profile
                    ?.isPublic == true,
            )
        }

    @Test
    fun `로그아웃 뒤 늦게 완료된 공개 설정 저장은 이전 프로필을 복원하지 않는다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val pending = CompletableDeferred<RamapResult<AccountProfile>>()
            profiles.visibilityPending = pending
            profiles.ignoreVisibilityCancellation = true
            val viewModel = MyTabViewModel(profiles, FakeReviewCommunityRepository(), FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.dispatch(MyTabIntent.SaveVisibility(true))
            runCurrent()
            profiles.sessionUserIds.value = null
            runCurrent()
            pending.complete(RamapResult.Success(AccountProfile("first", "느긋한차슈", isPublic = true)))
            runCurrent()

            assertNull(viewModel.uiState.value.userId)
            assertNull(viewModel.uiState.value.profile)
        }

    @Test
    fun `차단한 사용자가 있으면 대화상자를 열고 목록을 로드한다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val community =
                FakeReviewCommunityRepository(
                    blockedUsersResult = RamapResult.Success(listOf(PublicProfile("user1", "차단사용자1"))),
                )
            val viewModel = MyTabViewModel(profiles, community, FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(MyTabIntent.OpenBlockedUsers)
                runCurrent()

                val sideEffect = awaitItem()
                assertTrue(sideEffect is MyTabSideEffect.OpenBlockedUsersDialog)

                val state = viewModel.uiState.value
                assertEquals(1, state.blockedUsers.size)
                assertEquals("차단사용자1", state.blockedUsers.first().nickname)

                viewModel.dispatch(MyTabIntent.DismissBlockedUsers)
                runCurrent()

                assertEquals(0, viewModel.uiState.value.blockedUsers.size)
            }
        }

    @Test
    fun `차단한 사용자가 없으면 토스트 메시지를 표시하고 대화상자를 열지 않는다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val community = FakeReviewCommunityRepository(blockedUsersResult = RamapResult.Success(emptyList()))
            val viewModel = MyTabViewModel(profiles, community, FakePersonalizationRepository(), FakeMyTabReviewRepository())
            runCurrent()

            viewModel.sideEffect.test {
                viewModel.dispatch(MyTabIntent.OpenBlockedUsers)
                runCurrent()

                val sideEffect = awaitItem()
                assertTrue(sideEffect is MyTabSideEffect.ShowToast)
                assertEquals(
                    Res.string.review_blocked_users_empty,
                    sideEffect.data.message,
                )
            }
        }
}
