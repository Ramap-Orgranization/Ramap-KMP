package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.designsystem.resource.category.CategoryResourceMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryResourceMapperTest {
    @Test
    fun `모든 카테고리를 서로 다른 라벨 리소스로 매핑한다`() {
        val labels = Category.entries.map(CategoryResourceMapper::label)

        assertEquals(Category.entries.size, labels.toSet().size)
    }
}
