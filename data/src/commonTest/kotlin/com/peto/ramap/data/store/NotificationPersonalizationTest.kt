package com.peto.ramap.data.store

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.notification.NotificationType
import com.peto.ramap.domain.model.notification.NotificationTypeId
import com.peto.ramap.domain.repository.BookmarkRepository
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fake.FakeBookmarkRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeSubscribedShopRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPersonalizationTest {
    @Test
    fun `운영 알림만 선택한 매장이 공유 구독에 반영되고 전체 해제로 제거된다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val repository = FakeSubscribedShopRepository()
            val store = DefaultPersonalizationStore(FakeBookmarkRepository(setOf("bookmark")), repository, login)
            store.refresh()
            assertTrue(store.publishNotificationSettings(settings("test-account")))
            assertEquals(setOf("shop"), value(store).notificationShopIds)
            assertEquals(setOf("bookmark"), value(store).bookmarkedShopIds)
            store.clearShopNotifications("shop")
            assertEquals(listOf("shop"), repository.clearRequests)
            assertTrue(value(store).notificationShopIds.isEmpty())
        }

    @Test
    fun `표시 선택이 비어도 보존된 활성 선택이 있으면 공유 구독을 유지한다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val store = DefaultPersonalizationStore(FakeBookmarkRepository(), FakeSubscribedShopRepository(), login)
            store.refresh()
            val saved = settings("test-account")
            val hiddenOnly = saved.copy(selection = saved.catalog.clear(), hasHiddenEffectiveSubscription = true)
            assertTrue(store.publishNotificationSettings(hiddenOnly))
            assertEquals(setOf("shop"), value(store).notificationShopIds)
            store.publishNotificationSettings(hiddenOnly.copy(hasHiddenEffectiveSubscription = false))
            assertTrue(value(store).notificationShopIds.isEmpty())
        }

    @Test
    fun `다른 계정의 늦은 저장 결과는 공유 상태를 변경하지 않는다`() =
        runTest {
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED)
            val store = DefaultPersonalizationStore(FakeBookmarkRepository(), FakeSubscribedShopRepository(), login)
            store.refresh()
            login.updateAccountId("account-b")
            store.clear()
            assertFalse(store.publishNotificationSettings(settings("test-account")))
            assertEquals(PersonalizationBootstrapState.Loading, store.state.value)
        }

    @Test
    fun `조회 중 저장한 알림은 늦은 조회 결과로 덮어쓰지 않는다`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val bookmarkRepository =
                object : BookmarkRepository by FakeBookmarkRepository() {
                    override suspend fun fetchBookmarkedShopIds(accountId: String?): RamapResult<Set<String>> {
                        gate.await()
                        return RamapResult.Success(setOf("bookmark"))
                    }
                }
            val store = DefaultPersonalizationStore(bookmarkRepository, FakeSubscribedShopRepository(), FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            val refresh = launch { store.refresh() }
            yield()
            store.publishNotificationSettings(settings("test-account"))
            gate.complete(Unit)
            refresh.join()
            assertEquals(setOf("shop"), value(store).notificationShopIds)
            assertEquals(setOf("bookmark"), value(store).bookmarkedShopIds)
        }

    @Test
    fun `새로고침 전에 시작한 북마크 실패는 다른 저장을 지우지 않고 복구한다`() =
        runTest {
            val updateGate = CompletableDeferred<Unit>()
            val fetchGate = CompletableDeferred<Unit>()
            val bookmarks =
                object : BookmarkRepository by FakeBookmarkRepository() {
                    override suspend fun addBookmarks(
                        shopIds: Set<String>,
                        accountId: String?,
                    ): RamapResult<Unit> {
                        updateGate.await()
                        return RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
                    }

                    override suspend fun fetchBookmarkedShopIds(accountId: String?): RamapResult<Set<String>> {
                        fetchGate.await()
                        return RamapResult.Success(emptySet())
                    }
                }
            val store = DefaultPersonalizationStore(bookmarks, FakeSubscribedShopRepository(), FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            val update = launch { store.updateBookmark("failed-bookmark", true) }
            yield()
            val refresh = launch { store.refresh() }
            yield()
            store.publishNotificationSettings(settings("test-account"))
            updateGate.complete(Unit)
            update.join()
            fetchGate.complete(Unit)
            refresh.join()
            assertTrue(value(store).bookmarkedShopIds.isEmpty())
            assertEquals(setOf("shop"), value(store).notificationShopIds)
        }

    private fun settings(accountId: String): NotificationSettings {
        val catalog = NotificationCatalog(1, listOf(NotificationType(NotificationTypeId("operating-only"), "운영", "서버 유형", 1)))
        return NotificationSettings(accountId, "shop", catalog, catalog.selectAll())
    }

    private fun value(store: DefaultPersonalizationStore) = (store.state.value as PersonalizationBootstrapState.Success).value
}
