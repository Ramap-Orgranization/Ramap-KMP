package com.peto.ramap.ui.resource

import com.peto.ramap.designsystem.resource.shop.ShopOpeningResourceMapper
import com.peto.ramap.domain.model.shop.ShopOpening
import kotlinx.datetime.LocalDate
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.new_shop_opening_date
import ramap.shared.generated.resources.new_shop_opening_date_official
import ramap.shared.generated.resources.new_shop_opening_date_soft
import kotlin.test.Test
import kotlin.test.assertEquals

class ShopOpeningResourceMapperTest {
    @Test
    fun `오픈 종류별로 적절한 UiText 리소스와 날짜 인자를 반환한다`() {
        val date = LocalDate(2026, 10, 28)

        val openingUiText = ShopOpeningResourceMapper.openingDate(ShopOpening(date, ShopOpening.Kind.OPENING))
        assertEquals(Res.string.new_shop_opening_date, openingUiText.resource)
        assertEquals(listOf(10, 28), openingUiText.arguments)

        val officialUiText = ShopOpeningResourceMapper.openingDate(ShopOpening(date, ShopOpening.Kind.OFFICIAL))
        assertEquals(Res.string.new_shop_opening_date_official, officialUiText.resource)
        assertEquals(listOf(10, 28), officialUiText.arguments)

        val softUiText = ShopOpeningResourceMapper.openingDate(ShopOpening(date, ShopOpening.Kind.SOFT))
        assertEquals(Res.string.new_shop_opening_date_soft, softUiText.resource)
        assertEquals(listOf(10, 28), softUiText.arguments)
    }
}
