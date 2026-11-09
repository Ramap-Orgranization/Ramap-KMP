package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class DefaultPersonalizationStore(
    private val bookmarkRepository: BookmarkRepository,
    private val subscribedShopRepository: SubscribedShopRepository,
    private val loginRepository: LoginRepository,
) : PersonalizationStore {
    private val mutableState = MutableStateFlow<PersonalizationBootstrapState>(PersonalizationBootstrapState.Loading)
    override val state = mutableState.asStateFlow()
    private var lastPersonalization = Personalization()
    private var notificationVersion = 0L
    private var generation = 0L
    private var refreshGeneration = 0L
    private var ownerAccountId: String? = loginRepository.currentAccountId()
    private val bookmarkVersions = mutableMapOf<String, Long>()

    override suspend fun refresh(): RamapResult<Unit> {
        val account = loginRepository.currentAccountId()
        if (ownerAccountId != account) {
            generation += 1
            lastPersonalization = Personalization()
            bookmarkVersions.clear()
        }
        val requestGeneration = generation
        val requestRefreshGeneration = ++refreshGeneration
        ownerAccountId = account
        val initialBookmarkVersions = bookmarkVersions.toMap()
        val initialNotificationVersion = notificationVersion
        mutableState.value = PersonalizationBootstrapState.Loading
        return try {
            val (bookmarks, subscriptions) =
                coroutineScope {
                    val bookmarks = async { bookmarkRepository.fetchBookmarkedShopIds(account) }
                    val subscriptions =
                        async {
                            if (account == null) RamapResult.Success(emptySet()) else subscribedShopRepository.fetchSubscribedShopIds()
                        }
                    bookmarks.await() to subscriptions.await()
                }
            if (!isCurrent(account, requestGeneration) || requestRefreshGeneration != refreshGeneration) return staleResult()
            when {
                bookmarks is RamapResult.Error -> bookmarks.also { mutableState.value = PersonalizationBootstrapState.Error }
                subscriptions is RamapResult.Error -> subscriptions.also { mutableState.value = PersonalizationBootstrapState.Error }
                else -> {
                    var mergedBookmarks = (bookmarks as RamapResult.Success).data
                    for ((shopId, version) in bookmarkVersions) {
                        if (initialBookmarkVersions[shopId] == version) continue
                        mergedBookmarks = if (shopId in currentPersonalization.bookmarkedShopIds) mergedBookmarks + shopId else mergedBookmarks - shopId
                    }
                    val mergedSubscriptions =
                        if (initialNotificationVersion == notificationVersion) {
                            (subscriptions as RamapResult.Success).data
                        } else {
                            currentPersonalization.notificationShopIds
                        }
                    publish(Personalization(mergedBookmarks, mergedSubscriptions))
                    RamapResult.Success(Unit)
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            if (isCurrent(account, requestGeneration) && requestRefreshGeneration == refreshGeneration) mutableState.value = PersonalizationBootstrapState.Error
            RamapResult.Error(RamapError.Unknown(failure))
        }
    }

    override suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        val account = loginRepository.currentAccountId()
        val requestGeneration = generation
        if (!isCurrent(account, requestGeneration)) return staleResult()
        if (account == null) return updateGuestBookmark(shopId, enabled, requestGeneration)

        val wasBookmarked = shopId in currentPersonalization.bookmarkedShopIds
        val version = (bookmarkVersions[shopId] ?: 0) + 1
        bookmarkVersions[shopId] = version
        publish(currentPersonalization.changeBookmark(shopId, isBookmarked = enabled))
        val result = persistBookmark(shopId, enabled, account)
        if (!isCurrent(account, requestGeneration)) return staleResult()
        if (bookmarkVersions[shopId] == version) {
            bookmarkVersions[shopId] = version + 1
            val persistedBookmark = if (result is RamapResult.Success) enabled else wasBookmarked
            publish(currentPersonalization.changeBookmark(shopId, isBookmarked = persistedBookmark))
        }
        return result
    }

    private suspend fun updateGuestBookmark(
        shopId: String,
        enabled: Boolean,
        requestGeneration: Long,
    ): RamapResult<Unit> {
        val result = persistBookmark(shopId, enabled, accountId = null)
        if (!isCurrent(null, requestGeneration)) return staleResult()
        if (result is RamapResult.Success) {
            bookmarkVersions[shopId] = (bookmarkVersions[shopId] ?: 0) + 1
            if (mutableState.value !is PersonalizationBootstrapState.Success) return refresh()
            publish(currentPersonalization.changeBookmark(shopId, isBookmarked = enabled))
        }
        return result
    }

    private suspend fun persistBookmark(
        shopId: String,
        enabled: Boolean,
        accountId: String?,
    ): RamapResult<Unit> = if (enabled) bookmarkRepository.addBookmarks(setOf(shopId), accountId) else bookmarkRepository.removeBookmark(shopId, accountId)

    override suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit> = bookmarkRepository.synchronizeGuestBookmarks(accountId)

    override suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit> {
        val account = loginRepository.currentAccountId()
        val requestGeneration = generation
        if (!isCurrent(account, requestGeneration)) return staleResult()
        val newShopIds = shopIds - currentPersonalization.bookmarkedShopIds
        if (newShopIds.isEmpty()) return RamapResult.Success(Unit)
        val result = bookmarkRepository.addBookmarks(newShopIds, account)
        if (!isCurrent(account, requestGeneration)) return staleResult()
        if (result is RamapResult.Success) {
            newShopIds.forEach { bookmarkVersions[it] = (bookmarkVersions[it] ?: 0) + 1 }
            if (account == null && mutableState.value !is PersonalizationBootstrapState.Success) return refresh()
            publish(currentPersonalization.addBookmarks(newShopIds))
        }
        return result
    }

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        val account = loginRepository.currentAccountId()
        val requestGeneration = generation
        val result = if (enabled) subscribedShopRepository.subscribeShop(shopId) else subscribedShopRepository.unsubscribeShop(shopId)
        if (!isCurrent(account, requestGeneration)) return staleResult()
        if (result is RamapResult.Error) return result
        return when (val ids = subscribedShopRepository.fetchSubscribedShopIds()) {
            is RamapResult.Error -> ids
            is RamapResult.Success -> {
                if (!isCurrent(account, requestGeneration)) return staleResult()
                notificationVersion += 1
                publish(currentPersonalization.copy(notificationShopIds = ids.data))
                RamapResult.Success(Unit)
            }
        }
    }

    override fun publishNotificationSettings(settings: NotificationSettings): Boolean {
        if (settings.accountId != loginRepository.currentAccountId() || settings.accountId != ownerAccountId) return false
        notificationVersion += 1
        publish(currentPersonalization.changeNotificationSubscription(settings.shopId, settings.hasEffectiveSubscription))
        return true
    }

    override suspend fun clearShopNotifications(shopId: String): RamapResult<Unit> {
        val account = loginRepository.currentAccountId() ?: return staleResult()
        val requestGeneration = generation
        return when (val result = subscribedShopRepository.clearNotificationSettings(shopId, account)) {
            is RamapResult.Error -> result
            is RamapResult.Success -> {
                if (!isCurrent(account, requestGeneration) || !publishNotificationSettings(result.data)) return staleResult()
                RamapResult.Success(Unit)
            }
        }
    }

    override suspend fun clear() {
        generation += 1
        ownerAccountId = loginRepository.currentAccountId()
        bookmarkVersions.clear()
        lastPersonalization = Personalization()
        mutableState.value = PersonalizationBootstrapState.Loading
    }

    private fun isCurrent(
        account: String?,
        requestGeneration: Long,
    ): Boolean = account == loginRepository.currentAccountId() && account == ownerAccountId && requestGeneration == generation

    private fun staleResult(): RamapResult.Error = RamapResult.Error(RamapError.Unknown(IllegalStateException(STALE_SESSION)))

    private val currentPersonalization: Personalization
        get() = lastPersonalization

    private fun publish(personalization: Personalization) {
        lastPersonalization = personalization
        mutableState.value = PersonalizationBootstrapState.Success(personalization)
    }

    private companion object {
        const val STALE_SESSION = "Personalization session changed"
    }
}
