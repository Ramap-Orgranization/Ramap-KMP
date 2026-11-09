package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.announcement.AnnouncementReadState
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAnnouncementReadStoreTest {
    @Test
    fun `비로그인 기록과 각 계정의 기록을 분리하고 로그인 시 합치지 않는다`() =
        runTest {
            val login = FakeLoginRepository()
            val guest = GuestAnnouncementReadStorageFake()
            val remote = AnnouncementReadDataSourceFake()
            val store = DefaultAnnouncementReadStore(remote, login, guest)
            var state = AnnouncementReadState()
            backgroundScope.launch { store.state.collect { state = it } }
            runCurrent()
            store.markRead("guest", null)
            runCurrent()
            assertEquals(setOf("guest"), state.readIds)
            assertTrue(remote.reads.isEmpty())

            login.updateAccountId("first")
            runCurrent()
            assertEquals(AnnouncementReadState("first"), state)
            store.markRead("first-notice", "first")
            runCurrent()
            assertEquals(setOf("first-notice"), state.readIds)
            login.updateAccountId("second")
            runCurrent()
            assertEquals(AnnouncementReadState("second"), state)
            store.markRead("second-notice", "second")
            login.updateAccountId("first")
            runCurrent()
            assertEquals(setOf("first-notice"), state.readIds)
            login.updateAccountId(null)
            runCurrent()
            assertEquals(AnnouncementReadState(readIds = setOf("guest")), state)
            assertEquals(setOf("guest"), guest.readIds.value)
        }

    @Test
    fun `같은 계정의 다른 기기와 재생성된 저장소는 서버 읽음 기록을 가져온다`() =
        runTest {
            val remote = AnnouncementReadDataSourceFake()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "user")
            val first = DefaultAnnouncementReadStore(remote, login, GuestAnnouncementReadStorageFake())
            val second = DefaultAnnouncementReadStore(remote, login, GuestAnnouncementReadStorageFake())
            var secondState = AnnouncementReadState()
            backgroundScope.launch { second.state.collect { secondState = it } }
            runCurrent()
            first.markRead("notice", "user")
            assertTrue(secondState.readIds.isEmpty())
            second.refresh()
            runCurrent()
            assertEquals(setOf("notice"), secondState.readIds)
            val restored = DefaultAnnouncementReadStore(remote, login, GuestAnnouncementReadStorageFake())
            var restoredState = AnnouncementReadState()
            backgroundScope.launch { restored.state.collect { restoredState = it } }
            runCurrent()
            assertEquals(setOf("notice"), restoredState.readIds)
        }

    @Test
    fun `서버 저장 실패 시 읽음으로 표시하거나 비로그인 저장소에 대신 저장하지 않는다`() =
        runTest {
            val remote = AnnouncementReadDataSourceFake().apply { failWrite = true }
            val guest = GuestAnnouncementReadStorageFake()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "user")
            val store = DefaultAnnouncementReadStore(remote, login, guest)
            var state = AnnouncementReadState()
            backgroundScope.launch { store.state.collect { state = it } }
            runCurrent()
            assertIs<RamapResult.Error>(store.markRead("notice", "user"))
            runCurrent()
            assertTrue(state.readIds.isEmpty())
            assertTrue(guest.readIds.value.isEmpty())
            remote.failWrite = false
            store.markRead("notice", "user")
            runCurrent()
            assertEquals(setOf("notice"), state.readIds)
        }

    @Test
    fun `늦게 끝난 조회가 조회 도중 저장한 읽음 기록을 되돌리지 않는다`() =
        runTest {
            val remote = AnnouncementReadDataSourceFake()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "user")
            val store = DefaultAnnouncementReadStore(remote, login, GuestAnnouncementReadStorageFake())
            var state = AnnouncementReadState()
            backgroundScope.launch { store.state.collect { state = it } }
            runCurrent()
            val pending = CompletableDeferred<Set<String>>()
            remote.fetchPending = pending
            val refresh = launch { store.refresh() }
            runCurrent()
            store.markRead("notice", "user")
            pending.complete(emptySet())
            refresh.join()
            runCurrent()
            assertEquals(setOf("notice"), state.readIds)
        }

    @Test
    fun `계정 전환 뒤 완료된 저장 응답은 원래 계정에만 반영된다`() =
        runTest {
            val remote = AnnouncementReadDataSourceFake()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "first")
            val store = DefaultAnnouncementReadStore(remote, login, GuestAnnouncementReadStorageFake())
            var state = AnnouncementReadState()
            backgroundScope.launch { store.state.collect { state = it } }
            runCurrent()
            val pending = CompletableDeferred<Unit>()
            remote.writePending = pending
            val write = launch { store.markRead("notice", "first") }
            runCurrent()
            login.updateAccountId("second")
            runCurrent()
            pending.complete(Unit)
            write.join()
            runCurrent()
            assertEquals(AnnouncementReadState("second"), state)
            assertEquals(setOf("notice"), remote.reads["first"])
            assertTrue(remote.reads["second"].isNullOrEmpty())
        }

    @Test
    fun `이전 계정의 재시도는 현재 계정으로 저장하지 않는다`() =
        runTest {
            val remote = AnnouncementReadDataSourceFake()
            val guest = GuestAnnouncementReadStorageFake()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "second")
            val store = DefaultAnnouncementReadStore(remote, login, guest)
            assertIs<RamapResult.Error>(store.markRead("notice", "first"))
            assertIs<RamapResult.Error>(store.markRead("notice", null))
            assertTrue(remote.reads.isEmpty())
            assertTrue(guest.readIds.value.isEmpty())
        }
}
