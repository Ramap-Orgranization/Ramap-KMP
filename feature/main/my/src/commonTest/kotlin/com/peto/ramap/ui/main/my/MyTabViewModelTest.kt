package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import com.peto.ramap.ui.main.my.profile.FakeProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabViewModelTest {
    @Test
    fun `첫 세션 응답 전에는 프로필 표시를 보류하고 조회 시작 시 로딩 상태가 된다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val pending = CompletableDeferred<RamapResult<AccountProfile>>()
            profiles.fetchPending = pending
            val viewModel = MyTabViewModel(profiles, FakePersonalizationRepository())

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
            val viewModel = MyTabViewModel(profiles, personalization)
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
            val viewModel = MyTabViewModel(profiles, FakePersonalizationRepository())
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
            val viewModel = MyTabViewModel(profiles, FakePersonalizationRepository())
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
}
