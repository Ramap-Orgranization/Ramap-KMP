package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.NotificationDataSource
import com.peto.ramap.data.model.NotificationSettingsResponse
import com.peto.ramap.data.model.NotificationTypeResponse
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.fake.FakeLoginRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultSubscribedShopRepositoryTest {
    @Test
    fun `서버가 추가한 유형과 선택을 조회하고 같은 계정과 리비전으로 저장한다`() =
        runTest {
            val source = NotificationDataSourceFixture()
            val repository = DefaultSubscribedShopRepository(source, FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            val settings = assertIs<RamapResult.Success<*>>(repository.fetchNotificationSettings("shop", "test-account")).data as NotificationSettings
            assertEquals(listOf("event", "future-server-id"), settings.catalog.types.map { it.id.value })
            assertEquals(
                setOf("event"),
                settings.selection.ids
                    .map { it.value }
                    .toSet(),
            )
            repository.saveNotificationSettings(settings, settings.catalog.selectAll())
            assertEquals("test-account", source.savedAccount)
            assertEquals(12L, source.savedRevision)
            assertEquals(false, source.savedClearAll)
            assertEquals(setOf("event", "future-server-id"), source.savedIds)
            assertEquals(setOf("operating-only-shop"), assertIs<RamapResult.Success<Set<String>>>(repository.fetchSubscribedShopIds()).data)
        }

    @Test
    fun `전체 해제는 이벤트 전용 명령 대신 전체 설정 명령을 사용한다`() =
        runTest {
            val source = NotificationDataSourceFixture()
            val repository = DefaultSubscribedShopRepository(source, FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            repository.clearNotificationSettings("shop", "test-account")
            assertEquals("shop", source.clearedShop)
            assertEquals(0, source.eventWrites)
        }

    @Test
    fun `비활성화되거나 숨겨진 알림 유형이 선택 목록에 포함되어 있어도 안전하게 필터링된다`() =
        runTest {
            val source = NotificationDataSourceFixture()
            source.snapshot =
                NotificationSettingsResponse(
                    "test-account",
                    "shop",
                    1L,
                    listOf(
                        NotificationTypeResponse("active-id", "활성", "설명", 1, isActive = true, isVisible = true),
                        NotificationTypeResponse("inactive-id", "비활성", "설명", 2, isActive = false, isVisible = true),
                        NotificationTypeResponse("hidden-id", "숨김", "설명", 3, isActive = true, isVisible = false),
                    ),
                    setOf("active-id", "inactive-id", "hidden-id"),
                )
            val repository = DefaultSubscribedShopRepository(source, FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            val settings = assertIs<RamapResult.Success<*>>(repository.fetchNotificationSettings("shop", "test-account")).data as NotificationSettings

            assertEquals(listOf("active-id"), settings.catalog.types.map { it.id.value })
            assertEquals(
                setOf("active-id"),
                settings.selection.ids
                    .map { it.value }
                    .toSet(),
            )
        }

    @Test
    fun `숨겨진 유효 선택을 빈 표시 선택과 구분하고 명시적 전체 해제 의도를 전송한다`() =
        runTest {
            val source = NotificationDataSourceFixture()
            source.snapshot = source.snapshot.copy(selectedTypeIds = emptySet(), hasHiddenEffectiveSubscription = true)
            val repository = DefaultSubscribedShopRepository(source, FakeLoginRepository(LoginSessionState.AUTHENTICATED))
            val settings = assertIs<RamapResult.Success<NotificationSettings>>(repository.fetchNotificationSettings("shop", "test-account")).data
            assertEquals(emptySet(), settings.selection.ids)
            assertEquals(true, settings.hasEffectiveSubscription)
            val cleared = assertIs<RamapResult.Success<NotificationSettings>>(repository.saveNotificationSettings(settings, settings.catalog.clear(), clearAll = true)).data
            assertEquals(true, source.savedClearAll)
            assertEquals(false, cleared.hasEffectiveSubscription)
        }

    @Test
    fun `게스트 유저일 때 구독 매장 ID 조회 시 빈 집합을 반환한다`() =
        runTest {
            val source = NotificationDataSourceFixture()
            val repository = DefaultSubscribedShopRepository(source, FakeLoginRepository(LoginSessionState.NOT_AUTHENTICATED))
            val result = repository.fetchSubscribedShopIds()
            assertEquals(setOf<String>(), assertIs<RamapResult.Success<Set<String>>>(result).data)
        }
}

internal class NotificationDataSourceFixture : NotificationDataSource {
    var savedAccount: String? = null
    var savedRevision: Long? = null
    var savedClearAll: Boolean? = null
    var savedIds: Set<String>? = null
    var clearedShop: String? = null
    var eventWrites = 0
    var snapshot =
        NotificationSettingsResponse(
            "test-account",
            "shop",
            12,
            listOf(
                NotificationTypeResponse("future-server-id", "서버에서 변경한 제목", "설명", 20, true, true),
                NotificationTypeResponse("event", "이벤트", "설명", 10, true, true),
            ),
            setOf("event"),
        )

    override suspend fun fetchSettings(
        shopId: String,
        expectedAccountId: String,
    ) = snapshot

    override suspend fun saveSettings(
        shopId: String,
        expectedAccountId: String,
        revision: Long,
        selectedIds: Set<String>,
        clearAll: Boolean,
    ): NotificationSettingsResponse {
        savedAccount = expectedAccountId
        savedRevision = revision
        savedIds = selectedIds
        savedClearAll = clearAll
        return snapshot.copy(selectedTypeIds = selectedIds, hasHiddenEffectiveSubscription = !clearAll && snapshot.hasHiddenEffectiveSubscription)
    }

    override suspend fun clearSettings(
        shopId: String,
        expectedAccountId: String,
    ): NotificationSettingsResponse {
        clearedShop = shopId
        return snapshot.copy(selectedTypeIds = emptySet(), hasHiddenEffectiveSubscription = false)
    }

    override suspend fun fetchSubscribedShopIds(expectedAccountId: String) = setOf("operating-only-shop")

    override suspend fun changeEventSubscription(
        shopId: String,
        enabled: Boolean,
    ) {
        eventWrites += 1
    }
}
