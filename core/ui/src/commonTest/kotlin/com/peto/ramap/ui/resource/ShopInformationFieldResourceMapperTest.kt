package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.ui.resource.information.ShopInformationFieldResourceMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopInformationFieldResourceMapperTest {
    @Test
    fun `모든 신고 필드를 서로 다른 라벨 리소스로 매핑한다`() {
        val labels = ShopInformationField.entries.map(ShopInformationFieldResourceMapper::label)

        assertEquals(ShopInformationField.entries.size, labels.toSet().size)
    }
}
