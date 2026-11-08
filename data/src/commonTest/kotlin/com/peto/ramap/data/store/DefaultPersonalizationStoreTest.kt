package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fake.FakeBookmarkRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeSubscribedShopRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultPersonalizationStoreTest {
    @Test
    fun `초기 동기화 상태는 성공 전 로딩이고 성공 후 준비가 된다`() =
        runTest {
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(),
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            assertEquals(PersonalizationBootstrapState.Loading, store.state.value)
            store.refresh()
            assertEquals(
                PersonalizationBootstrapState.Success(Personalization()),
                store.state.value,
            )
        }

    @Test
    fun `초기 동기화 실패는 빈 값을 준비 상태로 확정하지 않는다`() =
        runTest {
            val bookmarkRepository =
                object : BookmarkRepository {
                    override suspend fun fetchBookmarkedShopIds(): RamapResult<Set<String>> = RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))

                    override suspend fun addBookmark(shopId: String) = RamapResult.Success(Unit)

                    override suspend fun addBookmarks(shopIds: Set<String>) = RamapResult.Success(Unit)

                    override suspend fun removeBookmark(shopId: String) = RamapResult.Success(Unit)
                }
            val store =
                DefaultPersonalizationStore(
                    bookmarkRepository,
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            assertIs<RamapResult.Error>(store.refresh())

            assertEquals(PersonalizationBootstrapState.Error, store.state.value)
        }

    @Test
    fun `초기 동기화 저장소가 예외를 던지면 오류 결과와 오류 상태로 전환한다`() =
        runTest {
            val failure = IllegalStateException("failure")
            val bookmarkRepository =
                object : BookmarkRepository {
                    override suspend fun fetchBookmarkedShopIds(): RamapResult<Set<String>> = throw failure

                    override suspend fun addBookmark(shopId: String) = RamapResult.Success(Unit)

                    override suspend fun addBookmarks(shopIds: Set<String>) = RamapResult.Success(Unit)

                    override suspend fun removeBookmark(shopId: String) = RamapResult.Success(Unit)
                }
            val store =
                DefaultPersonalizationStore(
                    bookmarkRepository,
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            val result = assertIs<RamapResult.Error>(store.refresh())

            val cause = assertIs<IllegalStateException>(result.error.cause)
            assertEquals(failure.message, cause.message)
            assertEquals(PersonalizationBootstrapState.Error, store.state.value)
        }

    @Test
    fun `새로고침 결과를 하나의 상태로 발행한다`() =
        runTest {
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(setOf("bookmark")),
                    FakeSubscribedShopRepository(setOf("notification")),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            assertIs<RamapResult.Success<Unit>>(store.refresh())

            assertEquals(
                Personalization(
                    bookmarkedShopIds = setOf("bookmark"),
                    notificationShopIds = setOf("notification"),
                ),
                personalization(store),
            )
        }

    @Test
    fun `상태 변경 실패시 낙관적 상태를 롤백한다`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository(shouldFailUpdate = true)
            val store =
                DefaultPersonalizationStore(
                    bookmarkRepository,
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            assertIs<RamapResult.Error>(store.updateBookmark("shop", true))

            assertEquals(Personalization(), personalization(store))
        }

    @Test
    fun `여러 매장 추가는 원격 bulk 저장을 사용한다`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val store =
                DefaultPersonalizationStore(
                    bookmarkRepository,
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(store.addBookmarks(setOf("shop-a", "shop-b")))

            assertEquals(setOf("shop-a", "shop-b"), bookmarkRepository.shopIds)
            assertEquals(listOf(setOf("shop-a", "shop-b")), bookmarkRepository.bulkAddRequests)
            assertEquals(
                setOf("shop-a", "shop-b"),
                personalization(store).bookmarkedShopIds,
            )
        }

    @Test
    fun `여러 매장 중 저장 실패가 발생하면 공유 상태를 갱신하지 않는다`() =
        runTest {
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(shouldFailUpdate = true),
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )
            store.refresh()

            assertIs<RamapResult.Error>(store.addBookmarks(setOf("shop-a", "shop-b")))

            assertEquals(Personalization(), personalization(store))
        }

    @Test
    fun `알림을 활성화하면 매장을 구독한다`() =
        runTest {
            val subscribedShopRepository = FakeSubscribedShopRepository()
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(),
                    subscribedShopRepository,
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )

            assertIs<RamapResult.Success<Unit>>(
                store.updateShopNotification("shop", true),
            )

            assertEquals(setOf("shop"), personalization(store).notificationShopIds)
            assertEquals(listOf("shop"), subscribedShopRepository.subscriptionRequests)
            assertEquals(emptyList(), subscribedShopRepository.unsubscriptionRequests)
        }

    @Test
    fun `알림을 비활성화하면 매장 구독을 해제한다`() =
        runTest {
            val subscribedShopRepository = FakeSubscribedShopRepository(setOf("shop"))
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(),
                    subscribedShopRepository,
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )
            store.refresh()

            assertIs<RamapResult.Success<Unit>>(
                store.updateShopNotification("shop", false),
            )

            assertEquals(emptySet(), personalization(store).notificationShopIds)
            assertEquals(emptyList(), subscribedShopRepository.subscriptionRequests)
            assertEquals(listOf("shop"), subscribedShopRepository.unsubscriptionRequests)
        }

    @Test
    fun `초기화하면 모든 개인화 상태를 비운다`() =
        runTest {
            val store =
                DefaultPersonalizationStore(
                    FakeBookmarkRepository(setOf("shop")),
                    FakeSubscribedShopRepository(),
                    FakeLoginRepository(LoginSessionState.AUTHENTICATED),
                )
            store.refresh()

            store.clear()

            assertEquals(Personalization(), personalization(store))
        }

    private fun personalization(store: DefaultPersonalizationStore): Personalization = assertIs<PersonalizationBootstrapState.Success>(store.state.value).value
}
