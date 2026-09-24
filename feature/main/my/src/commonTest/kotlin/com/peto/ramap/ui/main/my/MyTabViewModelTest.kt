package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.profile.AccountProfile
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.ui.main.my.profile.FakeProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabViewModelTest {
    @Test
    fun `프로필과 개인화 개수를 표시하고 세션이 바뀌면 프로필을 비운다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository()
            val personalization = FakePersonalizationRepository()
            val viewModel = MyTabViewModel(profiles, personalization)
            runCurrent()

            val initialProfile = viewModel.uiState.value.profile
            assertEquals("느긋한차슈", initialProfile?.nickname)
            personalization.updateBookmarkedShopIds(setOf("one", "two"))
            runCurrent()
            assertEquals(2, viewModel.uiState.value.bookmarkedCount)

            profiles.fetchResult = RamapResult.Success(AccountProfile("first", "새 닉네임"))
            viewModel.refresh()
            runCurrent()
            val updatedProfile = viewModel.uiState.value.profile
            assertEquals("새 닉네임", updatedProfile?.nickname)

            profiles.sessionUserIds.value = null
            runCurrent()
            assertNull(viewModel.uiState.value.profile)
        }
}
