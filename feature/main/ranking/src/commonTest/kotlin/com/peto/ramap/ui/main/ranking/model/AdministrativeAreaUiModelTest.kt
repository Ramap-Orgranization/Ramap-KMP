package com.peto.ramap.ui.main.ranking.model

import com.peto.ramap.domain.model.shop.AdministrativeArea
import kotlin.test.Test
import kotlin.test.assertEquals

class AdministrativeAreaUiModelTest {
    @Test
    fun `모든 행정 구역을 단축명과 공식 명칭 리소스에 한 번씩 매핑한다`() {
        val entries = AdministrativeAreaUiModel.entries

        assertEquals(17, entries.size)
        assertEquals(AdministrativeArea.entries.toSet(), entries.map { it.area }.toSet())
        assertEquals(entries.size, entries.map { it.shortNameResource }.toSet().size)
        assertEquals(entries.size, entries.map { it.officialNameResource }.toSet().size)
    }

    @Test
    fun `UI 모델 선택값은 도메인 행정 구역으로 왕복한다`() {
        AdministrativeArea.entries.forEach { area ->
            assertEquals(area, AdministrativeAreaUiModel.from(area).area)
        }
    }
}
