package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.platform.storage.GuestBookmarkStorage
import kotlinx.coroutines.CancellationException

internal class GuestBookmarkTransfer(
    private val dataSource: BookmarkShopDataSource,
    private val storage: GuestBookmarkStorage,
    private val loginRepository: LoginRepository,
) {
    suspend fun transfer(accountId: String) {
        val pending = storage.fetchPendingBookmarks(accountId)
        if (pending.isEmpty()) return

        val missing = pending - confirmBookmarks(accountId, pending)
        if (missing.isEmpty()) return

        for (batch in missing.chunked(BATCH_SIZE)) {
            val shopIds = batch.toSet()
            checkAccount(accountId)
            try {
                dataSource.addBookmarks(shopIds, accountId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                confirmBookmarks(accountId, shopIds)
                throw failure
            }

            val confirmed = confirmBookmarks(accountId, shopIds)
            check(confirmed == shopIds) { INCOMPLETE_TRANSFER }
        }
    }

    private suspend fun confirmBookmarks(
        accountId: String,
        pending: Set<String>,
    ): Set<String> {
        checkAccount(accountId)
        val confirmed = dataSource.fetchBookmarkedShopIds(accountId).intersect(pending)
        checkAccount(accountId)
        if (confirmed.isNotEmpty()) storage.forgetPendingBookmarks(accountId, confirmed)
        return confirmed
    }

    private fun checkAccount(accountId: String) {
        check(loginRepository.currentAccountId() == accountId) { STALE_SESSION }
    }

    private companion object {
        const val BATCH_SIZE = 100
        const val STALE_SESSION = "Bookmark transfer session changed"
        const val INCOMPLETE_TRANSFER = "Guest bookmarks were not confirmed by the account"
    }
}
