package com.peto.ramap.ui.resource

import com.peto.ramap.designsystem.resource.UiText
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.ui.resource.area.AdministrativeAreaUiModel
import com.peto.ramap.ui.resource.area.AreaFilterResourceMapper
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.ranking_all_regions
import ramap.shared.generated.resources.ranking_district_label
import kotlin.test.Test
import kotlin.test.assertEquals

class AreaFilterResourceMapperTest {
    @Test
    fun `전국과 선택 행정 구역 필터의 라벨을 매핑한다`() {
        assertEquals(
            UiText(Res.string.ranking_all_regions),
            AreaFilterResourceMapper.label(AreaFilter.Nationwide),
        )
        assertEquals(
            UiText(AdministrativeAreaUiModel.map(AdministrativeArea.SEOUL).shortName),
            AreaFilterResourceMapper.label(AreaFilter.Province(AdministrativeArea.SEOUL)),
        )
        assertEquals(
            UiText(Res.string.ranking_district_label, listOf("수원시")),
            AreaFilterResourceMapper.label(
                AreaFilter.District(
                    AdministrativeArea.GYEONGGI,
                    AdministrativeDistrict("수원시"),
                ),
            ),
        )
    }
}
