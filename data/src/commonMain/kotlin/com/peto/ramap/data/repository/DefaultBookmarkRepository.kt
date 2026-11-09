package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.network.execute.invokeRequest
import com.peto.ramap.platform.storage.GuestBookmarkStorage
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DefaultBookmarkRepository(
    private val dataSource: BookmarkShopDataSource,
    private val storage: GuestBookmarkStorage,
    private val loginRepository: LoginRepository,
) : BookmarkRepository {
    private val mutex = Mutex()
    private val guestBookmarkTransfer = GuestBookmarkTransfer(dataSource, storage, loginRepository)

    override suspend fun fetchBookmarkedShopIds(accountId: String?): RamapResult<Set<String>> =
        invokeRequest {
            checkAccount(accountId)
            val bookmarks = if (accountId == null) storage.fetchGuestBookmarks() else dataSource.fetchBookmarkedShopIds(accountId)
            checkAccount(accountId)
            bookmarks
        }

    override suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String?,
    ): RamapResult<Unit> =
        mutateBookmarks(accountId) {
            if (accountId == null) storage.addGuestBookmarks(shopIds) else dataSource.addBookmarks(shopIds, accountId)
        }

    override suspend fun removeBookmark(
        shopId: String,
        accountId: String?,
    ): RamapResult<Unit> =
        mutateBookmarks(accountId) {
            if (accountId == null) {
                storage.removeGuestBookmark(shopId)
            } else {
                storage.claimGuestBookmarks(accountId)
                checkAccount(accountId)
                storage.forgetPendingBookmarks(accountId, setOf(shopId))
                checkAccount(accountId)
                dataSource.removeBookmark(shopId, accountId)
            }
        }

    override suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit> =
        invokeRequest {
            checkAccount(accountId)
            // 계정 전환으로 원격 이전이 취소되어도 첫 계정에 귀속한 기록은 남긴다.
            withContext(NonCancellable) {
                mutex.withLock { storage.claimGuestBookmarks(accountId) }
            }
            mutex.withLock {
                checkAccount(accountId)
                guestBookmarkTransfer.transfer(accountId)
                checkAccount(accountId)
            }
        }

    private suspend fun mutateBookmarks(
        accountId: String?,
        mutation: suspend () -> Unit,
    ): RamapResult<Unit> =
        invokeRequest {
            mutex.withLock {
                checkAccount(accountId)
                mutation()
                checkAccount(accountId)
            }
        }

    private fun checkAccount(accountId: String?) {
        check(loginRepository.currentAccountId() == accountId) { STALE_SESSION }
    }

    private companion object {
        const val STALE_SESSION = "Bookmark session changed"
    }
}
