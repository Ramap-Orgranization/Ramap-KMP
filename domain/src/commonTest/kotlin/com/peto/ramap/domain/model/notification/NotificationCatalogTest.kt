package com.peto.ramap.domain.model.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationCatalogTest {
    @Test
    fun `새로운 서버 유형과 제목 및 순서를 그대로 사용한다`() {
        val catalog = NotificationCatalog(4, listOf(type("future", "새 서버 제목", 2), type("event", "이벤트", 1)))
        assertEquals(listOf("event", "future"), catalog.types.map { it.id.value })
        assertEquals("새 서버 제목", catalog.types.last().title)
        assertTrue(catalog.selectAll().contains(NotificationTypeId("future")))
        assertTrue(catalog.clear().ids.isEmpty())
    }

    @Test
    fun `새 항목이 추가되어도 저장한 선택을 자동으로 늘리지 않는다`() {
        val catalog = NotificationCatalog(2, listOf(type("event", "이벤트", 1), type("new", "신규", 2)))
        val selection = catalog.selection(setOf(NotificationTypeId("event")))
        assertFalse(selection.contains(NotificationTypeId("new")))
        assertTrue(catalog.selectAll().hasNewSelectionsComparedWith(selection))
        assertFalse(catalog.clear().hasNewSelectionsComparedWith(selection))
        assertEquals(catalog.clear(), selection.toggle(NotificationTypeId("event"), catalog))
    }

    @Test
    fun `카탈로그에 없는 선택과 중복 유형은 거절한다`() {
        val catalog = NotificationCatalog(1, listOf(type("event", "이벤트", 1)))
        assertFailsWith<IllegalArgumentException> { catalog.selection(setOf(NotificationTypeId("unknown"))) }
        assertFailsWith<IllegalArgumentException> { NotificationCatalog(1, listOf(type("same", "A", 1), type("same", "B", 2))) }
    }

    @Test
    fun `화면에 선택한 항목이 없어도 보존된 활성 구독은 유효하다`() {
        val catalog = NotificationCatalog(1, emptyList())
        val settings = NotificationSettings("account", "shop", catalog, catalog.clear(), hasHiddenEffectiveSubscription = true)
        assertTrue(settings.hasEffectiveSubscription)
        assertFalse(settings.copy(hasHiddenEffectiveSubscription = false).hasEffectiveSubscription)
    }

    private fun type(
        id: String,
        title: String,
        order: Int,
    ) = NotificationType(NotificationTypeId(id), title, "서버 설명", order)
}
