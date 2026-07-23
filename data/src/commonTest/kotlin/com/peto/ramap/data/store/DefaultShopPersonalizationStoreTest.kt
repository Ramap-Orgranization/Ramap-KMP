package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.fake.FakeBookmarkRepository
import com.peto.ramap.fake.FakeHiddenShopRepository
import com.peto.ramap.fake.FakeSubscribedShopRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultShopPersonalizationStoreTest {
    @Test
    fun `새로고침 결과를 하나의 상태로 발행한다`() =
        runTest {
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(setOf("bookmark")),
                    FakeHiddenShopRepository(setOf("hidden")),
                    FakeSubscribedShopRepository(setOf("notification")),
                )

            assertIs<RamapResult.Success<Unit>>(store.refresh())

            assertEquals(
                ShopPersonalization(
                    bookmarkedShopIds = setOf("bookmark"),
                    hiddenShopIds = setOf("hidden"),
                    notificationShopIds = setOf("notification"),
                ),
                store.state.value,
            )
        }

    @Test
    fun `숨김 처리하면 좋아요와 알림을 함께 해제한다`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(setOf("shop"))
            val hiddenShopRepository = FakeHiddenShopRepository()
            val subscribedShopRepository = FakeSubscribedShopRepository(setOf("shop"))
            val store =
                DefaultShopPersonalizationStore(
                    bookmarkRepository,
                    hiddenShopRepository,
                    subscribedShopRepository,
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(store.hideShop("shop"))

            assertEquals(ShopPersonalization(hiddenShopIds = setOf("shop")), store.state.value)
            assertEquals(setOf("shop"), bookmarkRepository.shopIds)
            assertEquals(emptyList(), bookmarkRepository.removeRequests)
            assertEquals(listOf("shop"), hiddenShopRepository.atomicHideRequests)
            assertEquals(setOf("shop"), hiddenShopRepository.shopIds)
            assertEquals(emptySet(), subscribedShopRepository.shopIds)
        }

    @Test
    fun `숨김 요청 실패시 원격 상태를 보상하고 로컬 상태를 롤백한다`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(setOf("shop"))
            val hiddenShopRepository = FakeHiddenShopRepository(shouldFailUpdate = true)
            val subscribedShopRepository = FakeSubscribedShopRepository(setOf("shop"))
            val store =
                DefaultShopPersonalizationStore(
                    bookmarkRepository,
                    hiddenShopRepository,
                    subscribedShopRepository,
                )
            store.refresh()
            val previous = store.state.value

            assertIs<RamapResult.Error>(store.hideShop("shop"))

            assertEquals(previous, store.state.value)
            assertEquals(setOf("shop"), bookmarkRepository.shopIds)
            assertEquals(setOf("shop"), subscribedShopRepository.shopIds)
        }

    @Test
    fun `원자 숨김 요청이 취소되면 구독과 로컬 상태를 복구하고 취소를 전파한다`() =
        runTest {
            val hideStarted = CompletableDeferred<Unit>()
            val hiddenShopRepository =
                FakeHiddenShopRepository(
                    onAtomicHide = {
                        hideStarted.complete(Unit)
                        awaitCancellation()
                    },
                )
            val subscribedShopRepository = FakeSubscribedShopRepository(setOf("shop"))
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(setOf("shop")),
                    hiddenShopRepository,
                    subscribedShopRepository,
                )
            store.refresh()
            val previous = store.state.value

            val hide = async { store.hideShop("shop") }
            hideStarted.await()
            hide.cancel()

            assertFailsWith<CancellationException> { hide.await() }
            assertEquals(previous, store.state.value)
            assertEquals(setOf("shop"), subscribedShopRepository.shopIds)
        }

    @Test
    fun `숨김 해제하면 숨김 상태만 해제한다`() =
        runTest {
            val hiddenShopRepository = FakeHiddenShopRepository(setOf("shop"))
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(),
                    hiddenShopRepository,
                    FakeSubscribedShopRepository(),
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(store.unhideShop("shop"))

            assertEquals(ShopPersonalization(), store.state.value)
            assertEquals(emptySet(), hiddenShopRepository.shopIds)
        }

    @Test
    fun `숨김 해제 요청 실패시 로컬 상태를 롤백한다`() =
        runTest {
            val hiddenShopRepository =
                FakeHiddenShopRepository(
                    initialShopIds = setOf("shop"),
                    shouldFailUpdate = true,
                )
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(),
                    hiddenShopRepository,
                    FakeSubscribedShopRepository(),
                )
            store.refresh()
            val previous = store.state.value

            assertIs<RamapResult.Error>(store.unhideShop("shop"))

            assertEquals(previous, store.state.value)
            assertEquals(setOf("shop"), hiddenShopRepository.shopIds)
        }

    @Test
    fun `상태 변경 실패시 낙관적 상태를 롤백한다`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(shouldFailUpdate = true)
            val store =
                DefaultShopPersonalizationStore(
                    bookmarkRepository,
                    FakeHiddenShopRepository(),
                    FakeSubscribedShopRepository(),
                )

            assertIs<RamapResult.Error>(store.updateBookmark("shop", true))

            assertEquals(ShopPersonalization(), store.state.value)
        }

    @Test
    fun `알림을 활성화하면 매장을 구독한다`() =
        runTest {
            val subscribedShopRepository = FakeSubscribedShopRepository()
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(),
                    FakeHiddenShopRepository(),
                    subscribedShopRepository,
                )

            assertIs<RamapResult.Success<Unit>>(
                store.updateShopNotification("shop", true),
            )

            assertEquals(setOf("shop"), store.state.value.notificationShopIds)
            assertEquals(listOf("shop"), subscribedShopRepository.subscriptionRequests)
            assertEquals(emptyList(), subscribedShopRepository.unsubscriptionRequests)
        }

    @Test
    fun `알림을 비활성화하면 매장 구독을 해제한다`() =
        runTest {
            val subscribedShopRepository = FakeSubscribedShopRepository(setOf("shop"))
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(),
                    FakeHiddenShopRepository(),
                    subscribedShopRepository,
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(
                store.updateShopNotification("shop", false),
            )

            assertEquals(emptySet(), store.state.value.notificationShopIds)
            assertEquals(emptyList(), subscribedShopRepository.subscriptionRequests)
            assertEquals(listOf("shop"), subscribedShopRepository.unsubscriptionRequests)
        }

    @Test
    fun `초기화하면 모든 개인화 상태를 비운다`() =
        runTest {
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(setOf("shop")),
                    FakeHiddenShopRepository(),
                    FakeSubscribedShopRepository(),
                )
            store.refresh()

            store.clear()

            assertEquals(ShopPersonalization(), store.state.value)
        }

    @Test
    fun `숨김 매장의 알림 활성화는 원격 요청 없이 무시한다`() =
        runTest {
            val subscribedShopRepository = FakeSubscribedShopRepository()
            val store =
                DefaultShopPersonalizationStore(
                    FakeBookmarkRepository(),
                    FakeHiddenShopRepository(setOf("hidden")),
                    subscribedShopRepository,
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(
                store.updateShopNotification("hidden", true),
            )

            assertEquals(emptySet(), store.state.value.notificationShopIds)
            assertEquals(emptyList(), subscribedShopRepository.subscriptionRequests)
        }

    @Test
    fun `진행 중인 새로고침 뒤 초기화하면 이전 사용자 상태를 다시 발행하지 않는다`() =
        runTest {
            val fetchStarted = CompletableDeferred<Unit>()
            val allowFetchCompletion = CompletableDeferred<Unit>()
            val bookmarkRepository =
                object : BookmarkRepository {
                    override suspend fun fetchBookmarkedShopIds(): RamapResult<Set<String>> {
                        fetchStarted.complete(Unit)
                        allowFetchCompletion.await()
                        return RamapResult.Success(setOf("previous-user-shop"))
                    }

                    override suspend fun addBookmark(shopId: String) = RamapResult.Success(Unit)

                    override suspend fun removeBookmark(shopId: String) = RamapResult.Success(Unit)
                }
            val store =
                DefaultShopPersonalizationStore(
                    bookmarkRepository,
                    FakeHiddenShopRepository(),
                    FakeSubscribedShopRepository(),
                )

            val refresh = async { store.refresh() }
            fetchStarted.await()
            val clear = async { store.clear() }
            runCurrent()
            allowFetchCompletion.complete(Unit)
            refresh.await()
            clear.await()

            assertEquals(ShopPersonalization(), store.state.value)
        }
}
