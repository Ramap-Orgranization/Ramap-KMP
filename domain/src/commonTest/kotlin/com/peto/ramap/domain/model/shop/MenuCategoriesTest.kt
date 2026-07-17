package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MenuCategoriesTest {
    @Test
    fun `카테고리를 하나라도 가지고 있으면 카테고리 보유 상태다`() {
        val menuCategories = MenuCategories(listOf(Category.SHOYU))

        assertTrue(menuCategories.hasCategory)
    }

    @Test
    fun `필터가 비어있으면 항상 일치한다`() {
        val menuCategories = MenuCategories(emptyList())

        assertTrue(menuCategories.matches(RamenShopFilter()))
    }

    @Test
    fun `필터와 같은 카테고리가 하나라도 있으면 일치한다`() {
        val menuCategories = MenuCategories(listOf(Category.MAZESOBA, Category.JIRO))

        assertTrue(menuCategories.matches(RamenShopFilter(setOf(Category.JIRO))))
    }

    @Test
    fun `필터와 같은 카테고리가 없으면 일치하지 않는다`() {
        val menuCategories = MenuCategories(listOf(Category.MAZESOBA))

        assertFalse(menuCategories.matches(RamenShopFilter(setOf(Category.JIRO))))
    }
}
