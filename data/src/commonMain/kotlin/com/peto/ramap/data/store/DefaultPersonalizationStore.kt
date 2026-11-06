package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 역할별 저장소를 조합해 [PersonalizationStore]를 제공하는 기본 구현.
 *
 * 역할별 저장소의 결과를 공유 개인화 상태로 발행하고, 원격 반영 실패 시 로컬 상태를 롤백한다.
 */
internal class DefaultPersonalizationStore(
    private val bookmarkRepository: BookmarkRepository,
    private val subscribedShopRepository: SubscribedShopRepository,
) : PersonalizationStore {
    private val _state =
        MutableStateFlow<PersonalizationBootstrapState>(PersonalizationBootstrapState.Loading)
    override val state = _state.asStateFlow()

    /**
     * 북마크와 구독의 최신 값을 병렬 조회해 일관된 개인화 상태로 한 번에 발행한다.
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
            val (bookmarks, subscribedShops) = fetchPersonalization()
            publishRefresh(bookmarks, subscribedShops)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            RamapResult.Error(RamapError.Unknown(throwable))
        }

    /**
     * 북마크와 구독 매장 ID를 병렬 조회한다.
     */
    private suspend fun fetchPersonalization() =
        coroutineScope {
            val bookmarks = async { bookmarkRepository.fetchBookmarkedShopIds() }
            val subscribedShops = async { subscribedShopRepository.fetchSubscribedShopIds() }
            Pair(bookmarks.await(), subscribedShops.await())
        }

    /**
     * 새로고침 결과가 모두 성공한 경우에만 하나의 상태로 발행한다.
     */
    private fun publishRefresh(
        bookmarks: RamapResult<Set<String>>,
        subscribedShops: RamapResult<Set<String>>,
    ): RamapResult<Unit> {
        if (bookmarks is RamapResult.Error) return bookmarks
        if (subscribedShops is RamapResult.Error) return subscribedShops

        publish(
            Personalization(
                bookmarkedShopIds = (bookmarks as RamapResult.Success).data,
                notificationShopIds = (subscribedShops as RamapResult.Success).data,
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
        val newShopIds = shopIds - previous.bookmarkedShopIds
        if (newShopIds.isEmpty()) return RamapResult.Success(Unit)

        val result = bookmarkRepository.addBookmarks(newShopIds)
        if (result is RamapResult.Success) publish(previous.addBookmarks(newShopIds))
        return result
    }

    /**
     * 매장 구독 상태를 낙관적으로 변경하고 원격 반영 실패 시 롤백한다.
     */
    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        val previous = currentPersonalization
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
        publish(Personalization())
    }

    /**
     * 원격 결과가 오류일 때만 로컬 상태를 복구하고 결과 타입과 값을 보존한다.
     */
    private fun <T> rollbackOnError(
        result: RamapResult<T>,
        previous: Personalization,
    ): RamapResult<T> {
        if (result is RamapResult.Error) publish(previous)
        return result
    }

    private val currentPersonalization: Personalization
        get() = (state.value as? PersonalizationBootstrapState.Success)?.value ?: Personalization()

    private fun publish(personalization: Personalization) {
        _state.value = PersonalizationBootstrapState.Success(personalization)
    }
}
