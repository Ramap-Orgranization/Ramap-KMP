package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RamenShopFilterTest {
    @Test
    fun `새로운 카테고리를 추가하면 필터에 추가된다`() {
        // given
        val filter = RamenShopFilter()

        // when
        val result = filter + Category.MAZESOBA

        // then
        assertTrue(Category.MAZESOBA in result)
    }

    @Test
    fun `기존 카테고리를 제거하면 필터에서 제거된다`() {
        // given
        val filter = RamenShopFilter(setOf(Category.MAZESOBA))

        // when
        val result = filter - Category.MAZESOBA

        // then
        assertFalse(Category.MAZESOBA in result)
    }

    @Test
    fun `clear를 호출하면 모든 카테고리가 제거된다`() {
        // given
        val filter =
            RamenShopFilter(
                setOf(
                    Category.MAZESOBA,
                    Category.JIRO,
                ),
            )

        // when
        val result = filter.clear()

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `비어있는 필터는 isEmpty가 true이다`() {
        val filter = RamenShopFilter()

        assertTrue(filter.isEmpty())
    }

    @Test
    fun `카테고리가 하나라도 있으면 isEmpty가 false이다`() {
        val filter = RamenShopFilter(setOf(Category.MAZESOBA))

        assertFalse(filter.isEmpty())
    }

    @Test
    fun `같은 카테고리를 여러 번 추가해도 하나만 유지된다`() {
        // given
        val filter = RamenShopFilter()

        // when
        val result = filter + Category.MAZESOBA + Category.MAZESOBA

        // then
        assertEquals(setOf(Category.MAZESOBA), result.toSet())
    }

    @Test
    fun `없는 카테고리를 제거해도 변경되지 않는다`() {
        // given
        val filter = RamenShopFilter(setOf(Category.JIRO))

        // when
        val result = filter - Category.MAZESOBA

        // then
        assertEquals(filter, result)
    }

    @Test
    fun `영업시간 필터가 선택되면 카테고리가 없어도 비어있지 않다`() {
        val filter = RamenShopFilter(isOpenSelected = true)

        assertFalse(filter.isEmpty())
    }
}
