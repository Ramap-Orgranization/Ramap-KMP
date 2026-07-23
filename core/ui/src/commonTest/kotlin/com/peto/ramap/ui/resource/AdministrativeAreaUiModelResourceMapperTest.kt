package com.peto.ramap.ui.resource

import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.ui.resource.area.AdministrativeAreaResourceMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AdministrativeAreaUiModelResourceMapperTest {
    @Test
    fun `모든 행정 구역을 단축명과 공식 명칭 리소스에 한 번씩 매핑한다`() {
        val entries = AdministrativeAreaResourceMapper.entries

        assertEquals(AdministrativeArea.entries.size, entries.size)
        assertEquals(AdministrativeArea.entries.toSet(), entries.map { it.area }.toSet())
        assertEquals(entries.size, entries.map { it.shortName }.toSet().size)
        assertEquals(entries.size, entries.map { it.officialName }.toSet().size)
    }

    @Test
    fun `리소스 선택값은 도메인 행정 구역을 유지한다`() {
        AdministrativeArea.entries.forEach { area ->
            assertEquals(area, AdministrativeAreaResourceMapper.map(area).area)
        }
    }
}
