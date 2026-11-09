package com.peto.ramap.ui.announcement

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.announcement.Announcement
import com.peto.ramap.domain.model.announcement.AnnouncementReadState
import com.peto.ramap.fake.FakeAnnouncementReadStore
import com.peto.ramap.fake.FakeAnnouncementRepository
import com.peto.ramap.ui.announcement.contract.AnnouncementIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementViewModelTest {
    @Test
    fun `목록 조회는 읽음 처리하지 않고 선택한 공지만 읽는다`() =
        coroutinesTest {
            val first = announcement("first")
            val second = announcement("second")
            val storage = FakeAnnouncementReadStore()
            val viewModel = AnnouncementViewModel(FakeAnnouncementRepository(RamapResult.Success(listOf(first, second))), storage)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.readIds
                    .isEmpty(),
            )

            viewModel.dispatch(AnnouncementIntent.Open(first.id))
            runCurrent()

            assertEquals(first, viewModel.uiState.value.selected)
            assertEquals(setOf(first.id), viewModel.uiState.value.readIds)
            assertTrue(second.isUnread(viewModel.uiState.value.readIds))
            viewModel.dispatch(AnnouncementIntent.Dismiss)
            runCurrent()
            assertNull(viewModel.uiState.value.selected)
            assertEquals(setOf(first.id), storage.state.value.readIds)
        }

    @Test
    fun `공지를 빠르게 연속으로 열어도 각각 읽음 처리한다`() =
        coroutinesTest {
            val storage = FakeAnnouncementReadStore()
            val viewModel =
                AnnouncementViewModel(
                    FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("first"), announcement("second")))),
                    storage,
                )
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open("first"))
            viewModel.dispatch(AnnouncementIntent.Open("second"))
            runCurrent()
            assertEquals(setOf("first", "second"), storage.state.value.readIds)
            assertEquals(
                "second",
                viewModel.uiState.value.selected
                    ?.id,
            )
        }

    @Test
    fun `목록에 없는 공지를 선택하면 대화상자를 열거나 읽음 처리하지 않는다`() =
        coroutinesTest {
            val storage = FakeAnnouncementReadStore()
            val viewModel = AnnouncementViewModel(FakeAnnouncementRepository(), storage)
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open("missing"))
            runCurrent()
            assertNull(viewModel.uiState.value.selected)
            assertTrue(
                storage.state.value.readIds
                    .isEmpty(),
            )
        }

    @Test
    fun `조회 실패 후 다시 시도하면 로딩과 오류 상태를 복구한다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Error(RamapError.Http(503)))
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            assertTrue(viewModel.uiState.value.failed)
            assertFalse(viewModel.uiState.value.loading)

            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.Retry)
            runCurrent()
            assertTrue(viewModel.uiState.value.loading)
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.failed)
            pending.complete(RamapResult.Success(listOf(announcement("new"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.loading)
            assertEquals(
                "new",
                viewModel.uiState.value.announcements
                    .single()
                    .id,
            )
        }

    @Test
    fun `새로고침에서 삭제된 공지의 대화상자를 닫고 신규 공지는 읽지 않은 상태로 둔다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("old"))))
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open("old"))
            runCurrent()
            repository.result = RamapResult.Success(listOf(announcement("new")))
            viewModel.dispatch(AnnouncementIntent.Refresh)
            runCurrent()
            assertNull(viewModel.uiState.value.selected)
            assertTrue(
                viewModel.uiState.value.announcements
                    .single()
                    .isUnread(viewModel.uiState.value.readIds),
            )
        }

    @Test
    fun `목록에 다시 진입해도 저장된 읽음 기록을 유지한다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(announcement("read"))))
            val storage = FakeAnnouncementReadStore()
            storage.markRead("read")
            val viewModel = AnnouncementViewModel(repository, storage)
            runCurrent()
            assertFalse(
                viewModel.uiState.value.announcements
                    .single()
                    .isUnread(viewModel.uiState.value.readIds),
            )
        }

    @Test
    fun `서버 읽음 저장 실패는 본문을 유지하고 새 공지 표시를 지우지 않는다`() =
        coroutinesTest {
            val notice = announcement("notice")
            val storage = FakeAnnouncementReadStore().apply { markReadResult = RamapResult.Error(RamapError.Http(503)) }
            val viewModel = AnnouncementViewModel(FakeAnnouncementRepository(RamapResult.Success(listOf(notice))), storage)
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open(notice.id))
            runCurrent()
            assertEquals(notice, viewModel.uiState.value.selected)
            assertTrue(notice.isUnread(viewModel.uiState.value.readIds))
        }

    @Test
    fun `계정이 바뀌면 열린 본문을 닫고 새 계정의 읽음 기록을 표시한다`() =
        coroutinesTest {
            val notice = announcement("notice")
            val storage = FakeAnnouncementReadStore().apply { state.value = AnnouncementReadState("first") }
            val viewModel = AnnouncementViewModel(FakeAnnouncementRepository(RamapResult.Success(listOf(notice))), storage)
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open(notice.id))
            runCurrent()
            assertFalse(notice.isUnread(viewModel.uiState.value.readIds))
            storage.state.value = AnnouncementReadState("second")
            runCurrent()
            assertNull(viewModel.uiState.value.selected)
            assertTrue(notice.isUnread(viewModel.uiState.value.readIds))
        }

    @Test
    fun `화면 복귀 중에는 기존 목록과 본문을 유지하고 새로고침 표시를 띄우지 않는다`() =
        coroutinesTest {
            val notice = announcement("notice")
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(notice)))
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            viewModel.dispatch(AnnouncementIntent.Open(notice.id))
            runCurrent()

            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.ReturnToScreen)
            runCurrent()

            assertFalse(viewModel.uiState.value.loading)
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(listOf(notice), viewModel.uiState.value.announcements)
            assertEquals(notice, viewModel.uiState.value.selected)

            val updated = notice.copy(bodyMarkdown = "수정된 공지")
            pending.complete(RamapResult.Success(listOf(updated)))
            runCurrent()
            assertEquals(updated, viewModel.uiState.value.selected)
            assertEquals(listOf(updated), viewModel.uiState.value.announcements)
        }

    @Test
    fun `빈 목록에서도 당겨서 새로고침할 때만 새로고침 표시가 나타난다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository()
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.Refresh)
            runCurrent()

            assertTrue(viewModel.uiState.value.isRefreshing)
            assertFalse(viewModel.uiState.value.loading)

            pending.complete(RamapResult.Success(listOf(announcement("new"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(listOf(announcement("new")), viewModel.uiState.value.announcements)
        }

    @Test
    fun `새로고침 실패 시 표시를 종료하고 기존 목록과 오류를 함께 유지한다`() =
        coroutinesTest {
            val notice = announcement("notice")
            val repository = FakeAnnouncementRepository(RamapResult.Success(listOf(notice)))
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.Refresh)
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            pending.complete(RamapResult.Error(RamapError.Http(503)))
            runCurrent()
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertTrue(viewModel.uiState.value.failed)
            assertEquals(listOf(notice), viewModel.uiState.value.announcements)
        }

    @Test
    fun `당겨서 새로고침 중 화면에 복귀해도 진행 중인 표시와 요청을 유지한다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository()
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.Refresh)
            runCurrent()

            repository.pending = null
            viewModel.dispatch(AnnouncementIntent.ReturnToScreen)
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            pending.complete(RamapResult.Success(listOf(announcement("refreshed"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(listOf(announcement("refreshed")), viewModel.uiState.value.announcements)
        }

    @Test
    fun `화면 복귀 조회 중 당겨서 새로고침하면 사용자 요청 결과를 유지한다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository()
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            val background = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = background
            viewModel.dispatch(AnnouncementIntent.ReturnToScreen)
            runCurrent()

            val refresh = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = refresh
            viewModel.dispatch(AnnouncementIntent.Refresh)
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshing)

            refresh.complete(RamapResult.Success(listOf(announcement("latest"))))
            runCurrent()
            background.complete(RamapResult.Success(listOf(announcement("stale"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(listOf(announcement("latest")), viewModel.uiState.value.announcements)
        }

    @Test
    fun `조회 오류 이후 화면 복귀에 성공하면 새로고침 표시 없이 오류를 해제한다`() =
        coroutinesTest {
            val repository = FakeAnnouncementRepository(RamapResult.Error(RamapError.Http(503)))
            val viewModel = AnnouncementViewModel(repository, FakeAnnouncementReadStore())
            runCurrent()
            val pending = CompletableDeferred<RamapResult<List<Announcement>>>()
            repository.pending = pending
            viewModel.dispatch(AnnouncementIntent.ReturnToScreen)
            runCurrent()

            assertTrue(viewModel.uiState.value.failed)
            assertFalse(viewModel.uiState.value.loading)
            assertFalse(viewModel.uiState.value.isRefreshing)

            pending.complete(RamapResult.Success(listOf(announcement("recovered"))))
            runCurrent()
            assertFalse(viewModel.uiState.value.failed)
            assertEquals(listOf(announcement("recovered")), viewModel.uiState.value.announcements)
        }

    private fun announcement(id: String): Announcement = Announcement(id, "공지", "# 제목\n\n**강조**와 [링크](https://example.com)", Instant.parse("2026-11-09T00:00:00Z"))
}
