package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.ui.resource.area.AdministrativeAreaResourceMapper
import com.peto.ramap.ui.resource.area.label
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions
import kotlin.test.Test
import kotlin.test.assertEquals

class AreaFilterResourceMapperTest {
    @Test
    fun `전국과 선택 행정 구역 필터의 라벨을 매핑한다`() {
        assertEquals(
            Res.string.ranking_all_regions,
            AreaFilter.Nationwide.label(),
        )
        assertEquals(
            AdministrativeAreaResourceMapper.map(AdministrativeArea.SEOUL).shortName,
            AreaFilter.Selected(AdministrativeArea.SEOUL).label(),
        )
    }
}
