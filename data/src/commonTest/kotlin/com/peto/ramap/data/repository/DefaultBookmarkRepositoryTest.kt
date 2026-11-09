package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.fake.FakeBookmarkShopDataSource
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBookmarkRepositoryTest {
    private val remote = FakeBookmarkShopDataSource()
    private val storage = GuestBookmarkStorageFake()
    private val login = FakeLoginRepository()
    private val repository = DefaultBookmarkRepository(remote, storage, login)

    @Test
    fun `비회원은 기기에서만 매장을 저장하고 해제한다`() =
        runTest {
            assertIs<RamapResult.Success<Unit>>(repository.addBookmarks(setOf("first", "second"), null))
            assertIs<RamapResult.Success<Unit>>(repository.removeBookmark("first", null))

            assertEquals(RamapResult.Success(setOf("second")), repository.fetchBookmarkedShopIds(null))
            assertEquals(emptyList(), remote.addRequests)
        }

    @Test
    fun `로그인하면 이미 저장한 매장을 건너뛰고 기존 목록에 추가한다`() =
        runTest {
            storage.addGuestBookmarks(setOf("existing", "guest"))
            remote.save("account-a", setOf("existing", "server-only"))
            login.updateAccountId("account-a")

            assertIs<RamapResult.Success<Unit>>(repository.synchronizeGuestBookmarks("account-a"))

            assertEquals(listOf("account-a" to setOf("guest")), remote.addRequests)
            assertEquals(setOf("existing", "guest", "server-only"), remote.fetchBookmarkedShopIds("account-a"))
            login.updateAccountId(null)
            assertEquals(RamapResult.Success(emptySet()), repository.fetchBookmarkedShopIds(null))
        }

    @Test
    fun `응답을 잃고 계정을 바꿔도 다른 계정에는 이전하지 않고 원래 계정에서 재시도한다`() =
        runTest {
            storage.addGuestBookmarks(setOf("first", "second"))
            remote.add = { accountId, _ ->
                remote.save(accountId, setOf("first"))
                error("Response lost")
            }
            login.updateAccountId("account-a")
            assertIs<RamapResult.Error>(repository.synchronizeGuestBookmarks("account-a"))

            val reopened = DefaultBookmarkRepository(remote, storage, login)
            login.updateAccountId("account-b")
            assertIs<RamapResult.Success<Unit>>(reopened.synchronizeGuestBookmarks("account-b"))
            assertEquals(emptySet(), remote.fetchBookmarkedShopIds("account-b"))

            remote.add = remote::save
            login.updateAccountId("account-a")
            assertIs<RamapResult.Success<Unit>>(reopened.synchronizeGuestBookmarks("account-a"))
            assertEquals(
                listOf("account-a" to setOf("first", "second"), "account-a" to setOf("second")),
                remote.addRequests,
            )
        }

    @Test
    fun `서버 저장 후 대기 기록 정리에 실패하면 재시도에서 같은 매장을 다시 쓰지 않는다`() =
        runTest {
            storage.addGuestBookmarks(setOf("guest"))
            login.updateAccountId("account-a")
            storage.beforeForget = { error("Local write failed") }

            assertIs<RamapResult.Error>(repository.synchronizeGuestBookmarks("account-a"))
            assertEquals(setOf("guest"), remote.fetchBookmarkedShopIds("account-a"))

            storage.beforeForget = {}
            assertIs<RamapResult.Success<Unit>>(repository.synchronizeGuestBookmarks("account-a"))
            assertEquals(listOf("account-a" to setOf("guest")), remote.addRequests)
        }

    @Test
    fun `이전 중 계정을 변경해도 늦은 저장은 원래 계정에만 반영한다`() =
        runTest {
            storage.addGuestBookmarks(setOf("guest"))
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            remote.add = { accountId, shopIds ->
                started.complete(Unit)
                release.await()
                remote.save(accountId, shopIds)
            }
            login.updateAccountId("account-a")
            val transfer = async { repository.synchronizeGuestBookmarks("account-a") }
            started.await()

            login.updateAccountId("account-b")
            val secondTransfer = async { repository.synchronizeGuestBookmarks("account-b") }
            release.complete(Unit)

            assertIs<RamapResult.Error>(transfer.await())
            assertIs<RamapResult.Success<Unit>>(secondTransfer.await())
            assertEquals(setOf("guest"), remote.fetchBookmarkedShopIds("account-a"))
            assertEquals(emptySet(), remote.fetchBookmarkedShopIds("account-b"))
        }

    @Test
    fun `로그인 전에 시작한 기기 저장이 끝난 뒤 첫 계정에 이전한다`() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            storage.beforeAdd = {
                started.complete(Unit)
                release.await()
            }
            val save = async { repository.addBookmarks(setOf("guest"), null) }
            started.await()
            login.updateAccountId("account-a")
            val transfer = async { repository.synchronizeGuestBookmarks("account-a") }
            runCurrent()
            release.complete(Unit)

            assertIs<RamapResult.Error>(save.await())
            assertIs<RamapResult.Success<Unit>>(transfer.await())
            assertEquals(setOf("guest"), remote.fetchBookmarkedShopIds("account-a"))
        }

    @Test
    fun `이전 중 해제는 서버 저장 이후 실행되고 재시도로 되살리지 않는다`() =
        runTest {
            storage.addGuestBookmarks(setOf("guest"))
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            remote.add = { accountId, shopIds ->
                started.complete(Unit)
                release.await()
                remote.save(accountId, shopIds)
            }
            login.updateAccountId("account-a")
            val transfer = async { repository.synchronizeGuestBookmarks("account-a") }
            started.await()
            val removal = async { repository.removeBookmark("guest", "account-a") }
            runCurrent()
            release.complete(Unit)

            assertIs<RamapResult.Success<Unit>>(transfer.await())
            assertIs<RamapResult.Success<Unit>>(removal.await())
            assertIs<RamapResult.Success<Unit>>(repository.synchronizeGuestBookmarks("account-a"))
            assertEquals(emptySet(), remote.fetchBookmarkedShopIds("account-a"))
            assertEquals(listOf("account-a" to setOf("guest")), remote.addRequests)
        }
}
