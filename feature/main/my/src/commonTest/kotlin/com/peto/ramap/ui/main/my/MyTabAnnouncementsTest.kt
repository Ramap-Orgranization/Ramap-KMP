package com.peto.ramap.ui.main.my

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.fake.FakeAnnouncementReadStore
import com.peto.ramap.fake.FakeAnnouncementRepository
import com.peto.ramap.fake.FakeFollowRepository
import com.peto.ramap.fake.FakePersonalizationRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.ui.main.my.contract.MyTabIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabAnnouncementsTest {
    @Test
    fun `계정 전환 시 이전 소유자의 읽음 기록으로 새 계정 배지를 지우지 않는다`() =
        coroutinesTest {
            val profiles = FakeProfileRepository().apply { sessionUserId.value = null }
            val storage = FakeAnnouncementReadStore()
            val viewModel =
                MyTabViewModel(
                    profiles,
                    FakeUserModerationRepository(),
                    FakePersonalizationRepository(),
                    FakeMyTabReviewRepository(),
                    FakeFollowRepository(),
                    FakeReviewChangeStore(),
                    FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("first")))),
                    storage,
                )
            runCurrent()
            assertTrue(viewModel.uiState.value.sessionResolved)
            assertTrue(viewModel.uiState.value.hasUnreadAnnouncements)
            storage.markRead("first")
            runCurrent()
            profiles.sessionUserId.value = "signed-in"
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadAnnouncements)
            assertTrue(
                viewModel.uiState.value.readAnnouncementIds
                    .contains("first"),
            )
        }

    @Test
    fun `본문을 읽으면 마이 화면 배지가 즉시 사라지고 새 공지를 조회하면 다시 나타난다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("first"))))
            val storage = FakeAnnouncementReadStore()
            val viewModel = createViewModel(repository, storage)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadAnnouncements)

            storage.markRead("first")
            runCurrent()
            assertFalse(viewModel.uiState.value.hasUnreadAnnouncements)

            repository.result = RamapResult.Success(listOf(announcement("new"), announcement("first")))
            viewModel.dispatch(MyTabIntent.RefreshAnnouncements)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadAnnouncements)
        }

    @Test
    fun `새로고침 실패는 기존 공지 배지를 지우지 않는다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("first"))))
            val viewModel = createViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            repository.result = RamapResult.Error(RamapError.Http(503))
            viewModel.dispatch(MyTabIntent.RefreshAnnouncements)
            runCurrent()
            assertTrue(viewModel.uiState.value.hasUnreadAnnouncements)
        }

    private fun createViewModel(
        repository: FakeAnnouncementRepository,
        storage: FakeAnnouncementReadStore,
    ): MyTabViewModel =
        MyTabViewModel(
            FakeProfileRepository().apply { sessionUserId.value = null },
            FakeUserModerationRepository(),
            FakePersonalizationRepository(),
            FakeMyTabReviewRepository(),
            FakeFollowRepository(),
            FakeReviewChangeStore(),
            repository,
            storage,
        )

    private fun announcement(id: String): Announcement = Announcement(id, "공지", "본문", Instant.parse("2026-11-09T00:00:00Z"))
}
