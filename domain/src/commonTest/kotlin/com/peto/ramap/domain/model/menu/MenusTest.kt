package com.peto.ramap.domain.model.menu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MenusTest {
    @Test
    fun `메뉴 목록처럼 순서와 항목에 접근할 수 있다`() {
        val first = menu(id = "first")
        val second = menu(id = "second")
        val items = Menus(listOf(first, second))

        assertEquals(2, items.size)
        assertEquals(first, items.first())
        assertEquals(second, items.last())
        assertTrue(items.isNotEmpty())
    }

    private fun menu(id: String) =
        Menu(
            id = id,
            name = "메뉴",
            description = null,
            imageUrl = null,
            displayOrder = 0,
        )
}
