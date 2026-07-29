package com.peto.ramap.domain.model.shop

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryTest {
    @Test
    fun `창케 식별자를 카테고리로 변환한다`() {
        assertEquals(Category.CHANKE, Category.fromId("chanke"))
    }
}
