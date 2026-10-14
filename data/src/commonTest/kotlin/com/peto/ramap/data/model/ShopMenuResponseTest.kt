package com.peto.ramap.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShopMenuResponseTest {
    @Test
    fun `메뉴 상세 필드와 항목 순서를 유지하고 빈 섹션 및 소속 없는 항목은 제외한다`() {
        val response =
            ShopMenuResponse(
                menuSections =
                    listOf(
                        MenuSectionResponse("empty", "shop", "빈 섹션", displayOrder = 0),
                        MenuSectionResponse("section", "shop", "한정 메뉴", description = "하루 10그릇 한정", displayOrder = 1),
                    ),
                menuItems =
                    listOf(
                        MenuResponse("second", "section", "토핑", priceText = "별도 문의", displayOrder = 2),
                        MenuResponse(
                            id = "first",
                            sectionId = "section",
                            name = "시오라멘",
                            priceKrw = 10000,
                            description = "소금 육수",
                            imageUrl = "https://example.com/menu.jpg",
                            sourceUrl = "https://example.com/menu",
                            displayOrder = 1,
                            isRepresentative = true,
                        ),
                        MenuResponse("orphan", "missing", "소속 없는 메뉴", displayOrder = 0),
                    ),
                menuUpdatedAt = "2026-10-14T00:00:00Z",
            )

        val menu = response.toDomain()
        val section = menu.sections.single()
        val first = section.items.first()

        assertEquals("section", section.id)
        assertEquals("하루 10그릇 한정", section.description)
        assertEquals(listOf("first", "second"), section.items.map { it.id })
        assertEquals("시오라멘", first.name)
        assertEquals(10000, first.priceKrw?.krw)
        assertEquals("소금 육수", first.description)
        assertEquals("https://example.com/menu.jpg", first.imageUrl)
        assertEquals("https://example.com/menu", first.sourceUrl)
        assertEquals(true, first.isRepresentative)
        assertEquals("별도 문의", section.items.last().priceText)
        assertEquals(2, menu.itemCount)
        assertEquals("2026-10-14T00:00:00Z", menu.updatedAt)
    }
}
