package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 역할별 저장소를 조합해 [ShopPersonalizationStore]를 제공하는 기본 구현.
 *
 * 역할별 저장소의 결과를 공유 개인화 상태로 발행하고, 원격 반영 실패 시 로컬 상태를 롤백한다.
 */
internal class DefaultShopPersonalizationStore(
    private val bookmarkRepository: BookmarkRepository,
    private val hiddenShopRepository: HiddenShopRepository,
    private val subscribedShopRepository: SubscribedShopRepository,
) : ShopPersonalizationStore {
    private val _state =
        MutableStateFlow<PersonalizationBootstrapState>(PersonalizationBootstrapState.Loading)
    override val state = _state.asStateFlow()

    /**
     * 세 저장소의 최신 값을 병렬 조회해 일관된 개인화 상태로 한 번에 발행한다.
     *
     * 어느 한 조회라도 실패하면 기존 상태를 유지하고 해당 오류를 반환한다.
     */
    override suspend fun refresh(): RamapResult<Unit> {
        _state.value = PersonalizationBootstrapState.Loading
        return fetchAndPublishRefresh().also { result ->
            if (result is RamapResult.Error) _state.value = PersonalizationBootstrapState.Error
        }
    }

    private suspend fun fetchAndPublishRefresh(): RamapResult<Unit> =
        try {
            val (bookmarks, hiddenShops, subscribedShops) = fetchPersonalization()
            publishRefresh(bookmarks, hiddenShops, subscribedShops)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            RamapResult.Error(RamapError.Unknown(throwable))
        }

    /**
     * 북마크, 숨김, 구독한 매장 ID를 병렬 조회한다.
     */
    private suspend fun fetchPersonalization() =
        coroutineScope {
            val bookmarks = async { bookmarkRepository.fetchBookmarkedShopIds() }
            val hiddenShops = async { hiddenShopRepository.fetchHiddenShopIds() }
            val subscribedShops = async { subscribedShopRepository.fetchSubscribedShopIds() }
            Triple(bookmarks.await(), hiddenShops.await(), subscribedShops.await())
        }

    /**
     * 새로고침 결과가 모두 성공한 경우에만 하나의 상태로 발행한다.
     *
     * 숨긴 매장은 북마크와 구독 집합에서 제외하며, 실패 시 가장 먼저 확인된 오류를 반환한다.
     */
    private fun publishRefresh(
        bookmarks: RamapResult<Set<String>>,
        hiddenShops: RamapResult<Set<String>>,
        subscribedShops: RamapResult<Set<String>>,
    ): RamapResult<Unit> {
        if (bookmarks is RamapResult.Error) return bookmarks
        if (hiddenShops is RamapResult.Error) return hiddenShops
        if (subscribedShops is RamapResult.Error) return subscribedShops

        val hiddenShopIds = (hiddenShops as RamapResult.Success).data
        publish(
            ShopPersonalization(
                bookmarkedShopIds = (bookmarks as RamapResult.Success).data - hiddenShopIds,
                hiddenShopIds = hiddenShopIds,
                notificationShopIds = (subscribedShops as RamapResult.Success).data - hiddenShopIds,
            ),
        )
        return RamapResult.Success(Unit)
    }

    /**
     * 북마크 상태를 낙관적으로 변경하고 원격 반영 실패 시 변경 전 상태로 롤백한다.
     */
    override suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        val previous = currentPersonalization
        publish(previous.changeBookmark(shopId, isBookmarked = enabled))

        val result =
            if (enabled) {
                bookmarkRepository.addBookmark(shopId)
            } else {
                bookmarkRepository.removeBookmark(shopId)
            }

        return rollbackOnError(result, previous)
    }

    /** 모든 원격 북마크 저장이 성공한 뒤에만 공유 개인화 상태를 갱신한다. */
    override suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit> {
        val previous = currentPersonalization
        val newShopIds = shopIds - previous.hiddenShopIds - previous.bookmarkedShopIds
        if (newShopIds.isEmpty()) return RamapResult.Success(Unit)

        val result = addBookmarksRemotely(newShopIds)
        if (result is RamapResult.Success) publish(previous.addBookmarks(newShopIds))
        return result
    }

    private suspend fun addBookmarksRemotely(shopIds: Set<String>): RamapResult<Unit> = bookmarkRepository.addBookmarks(shopIds)

    /**
     * 매장을 숨기고 연관된 구독과 북마크를 함께 해제한다.
     *
     * 원격 처리 중 실패하면 완료된 변경을 가능한 범위에서 보상하고 로컬 상태 전체를 복구한다.
     */
    override suspend fun hideShop(shopId: String): RamapResult<Unit> = hideShopInternal(shopId)

    /**
     * 매장의 숨김 상태만 낙관적으로 해제하고 원격 요청 실패 시 이전 상태를 복구한다.
     */
    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = unhideShopInternal(shopId)

    /** 숨김을 해제하고 원격 요청 실패 시 이전 상태를 복구한다. */
    private suspend fun unhideShopInternal(shopId: String): RamapResult<Unit> {
        val previous = currentPersonalization
        publish(previous.copy(hiddenShopIds = previous.hiddenShopIds - shopId))
        return rollbackOnError(hiddenShopRepository.unhideShop(shopId), previous)
    }

    /**
     * 구독을 해제한 뒤 숨김과 북마크 제거를 원자적으로 처리한다.
     *
     * 숨김 실패나 코루틴 취소 시 구독과 로컬 상태를 취소 불가능한 구간에서 복구한다.
     */
    private suspend fun hideShopInternal(shopId: String): RamapResult<Unit> {
        val previous = currentPersonalization
        val hadBookmark = shopId in previous.bookmarkedShopIds
        val hadNotification = shopId in previous.notificationShopIds
        publish(previous.hideShop(shopId))

        return try {
            removeSubscriptionOrRollback(shopId, hadNotification, previous)?.let { return it }
            hideRemotely(shopId, hadBookmark).also { result ->
                if (result is RamapResult.Error) {
                    restoreAfterHideFailure(shopId, hadNotification, previous)
                }
            }
        } catch (cancellation: CancellationException) {
            restoreAfterHideFailure(shopId, hadNotification, previous)
            throw cancellation
        }
    }

    /**
     * 매장 구독 상태를 낙관적으로 변경하고 원격 반영 실패 시 롤백한다.
     *
     * 숨긴 매장의 구독 활성화 요청은 상태 불변 조건을 지키기 위해 성공한 no-op으로 처리한다.
     */
    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        val previous = currentPersonalization
        if (previous.shouldIgnoreNotificationUpdate(shopId, enabled)) {
            return RamapResult.Success(Unit)
        }
        publish(previous.changeNotificationSubscription(shopId, isSubscribed = enabled))
        val result = changeSubscriptionRemotely(shopId, shouldSubscribe = enabled)
        return rollbackOnError(result, previous)
    }

    /**
     * 요청 상태에 따라 구독 또는 구독 해제 명령을 명시적으로 호출한다.
     */
    private suspend fun changeSubscriptionRemotely(
        shopId: String,
        shouldSubscribe: Boolean,
    ): RamapResult<Unit> =
        if (shouldSubscribe) {
            subscribedShopRepository.subscribeShop(shopId)
        } else {
            subscribedShopRepository.unsubscribeShop(shopId)
        }

    /**
     * 저장된 개인화 상태를 비운다.
     *
     * 로그아웃 시 공유 개인화 상태를 초기화한다.
     */
    override suspend fun clear() {
        publish(ShopPersonalization())
    }

    /**
     * 기존 구독이 있던 매장만 구독 해제하며, 실패하면 즉시 로컬 상태를 롤백한다.
     *
     * @return 작업을 중단해야 하는 오류 또는 다음 단계를 진행할 수 있음을 나타내는 `null`
     */
    private suspend fun removeSubscriptionOrRollback(
        shopId: String,
        hadNotification: Boolean,
        previous: ShopPersonalization,
    ): RamapResult.Error? {
        if (!hadNotification) return null
        val result = subscribedShopRepository.unsubscribeShop(shopId)
        return (result as? RamapResult.Error)?.let { rollback(it, previous) }
    }

    /**
     * 북마크 여부에 따라 원자 숨김 API 또는 일반 숨김 API를 호출한다.
     */
    private suspend fun hideRemotely(
        shopId: String,
        hadBookmark: Boolean,
    ): RamapResult<Unit> =
        if (hadBookmark) {
            hiddenShopRepository.hideBookmarkedShop(shopId)
        } else {
            hiddenShopRepository.hideShop(shopId)
        }

    /**
     * 숨김 실패나 취소 후 구독과 로컬 상태를 반드시 복구한다.
     */
    private suspend fun restoreAfterHideFailure(
        shopId: String,
        hadNotification: Boolean,
        previous: ShopPersonalization,
    ) {
        withContext(NonCancellable) {
            restoreSubscription(shopId, hadNotification)
            publish(previous)
        }
    }

    /**
     * 숨김 처리 전에 구독이 존재했던 경우에만 보상 요청으로 다시 활성화한다.
     */
    private suspend fun restoreSubscription(
        shopId: String,
        shouldRestore: Boolean,
    ) {
        if (shouldRestore) subscribedShopRepository.subscribeShop(shopId)
    }

    /**
     * 로컬 상태를 이전 값으로 되돌리고 원래 오류를 그대로 반환한다.
     */
    private fun rollback(
        result: RamapResult.Error,
        previous: ShopPersonalization,
    ): RamapResult.Error {
        publish(previous)
        return result
    }

    /**
     * 원격 결과가 오류일 때만 로컬 상태를 복구하고 결과 타입과 값을 보존한다.
     */
    private fun <T> rollbackOnError(
        result: RamapResult<T>,
        previous: ShopPersonalization,
    ): RamapResult<T> {
        if (result is RamapResult.Error) publish(previous)
        return result
    }

    private val currentPersonalization: ShopPersonalization
        get() = (state.value as? PersonalizationBootstrapState.Success)?.value ?: ShopPersonalization()

    private fun publish(personalization: ShopPersonalization) {
        _state.value = PersonalizationBootstrapState.Success(personalization)
    }
}
