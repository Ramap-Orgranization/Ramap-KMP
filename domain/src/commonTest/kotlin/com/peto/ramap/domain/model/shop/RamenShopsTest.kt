package com.peto.ramap.domain.model.shop

import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RamenShopsTest {
    @Test
    fun `이미 렌더링된 가게는 제외한다`() {
        // given
        val renderedShopIds = mutableSetOf("1")
        val shops =
            RamenShops(
                mapOf(
                    "1" to ramenShopFixture(id = "1", name = "이미 렌더링된 가게"),
                    "2" to ramenShopFixture(id = "2", name = "새로운 가게"),
                ),
            )

        // when
        val result = shops.filterNotContainShops(renderedShopIds)

        // then
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `필터가 비어있으면 모든 가게를 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to
                        ramenShopFixture(
                            id = "1",
                            menuCategories = listOf(Category.MAZESOBA),
                        ),
                    "2" to
                        ramenShopFixture(
                            id = "2",
                            menuCategories = listOf(Category.JIRO),
                        ),
                ),
            )

        // when
        val result = shops.filterByCategory(RamenShopFilter())

        // then
        assertEquals(shops, result)
    }

    @Test
    fun `선택한 카테고리를 가진 가게만 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to
                        ramenShopFixture(
                            id = "1",
                            menuCategories = listOf(Category.MAZESOBA),
                        ),
                    "2" to
                        ramenShopFixture(
                            id = "2",
                            menuCategories = listOf(Category.JIRO),
                        ),
                    "3" to
                        ramenShopFixture(
                            id = "3",
                            menuCategories = emptyList(),
                        ),
                ),
            )
        val filter = RamenShopFilter(setOf(Category.MAZESOBA))

        // when
        val result = shops.filterByCategory(filter)

        // then
        assertEquals(setOf("1"), result.keys)
    }

    @Test
    fun `가게가 여러 카테고리를 가지고 있으면 하나만 일치해도 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to
                        ramenShopFixture(
                            id = "1",
                            menuCategories = listOf(Category.MAZESOBA, Category.JIRO),
                        ),
                    "2" to
                        ramenShopFixture(
                            id = "2",
                            menuCategories = emptyList(),
                        ),
                ),
            )
        val filter = RamenShopFilter(setOf(Category.JIRO))

        // when
        val result = shops.filterByCategory(filter)

        // then
        assertTrue("1" in result.keys)
        assertFalse("2" in result.keys)
    }

    @Test
    fun `영업중 필터는 현재 영업중인 매장만 반환한다`() {
        val openShop =
            ramenShopFixture(id = "break-time").copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("mon" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = mapOf("mon" to listOf(BusinessHoursBreakTime("15:00", "17:00"))),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val closedShop =
            ramenShopFixture(id = "closed-shop", menuCategories = listOf(Category.JIRO)).copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("mon" to BusinessHoursDay(true, "11:00", "01:00", true, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )

        val result =
            RamenShops(listOf(openShop, closedShop)).filterBy(
                RamenShopFilter(isOpenSelected = true),
                LocalDateTime(2026, 8, 10, 12, 0),
            )

        assertEquals(setOf("break-time"), result.keys)
    }

    @Test
    fun `영업중 필터는 브레이크타임과 영업 종료 후 매장을 제외한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("mon" to BusinessHoursDay(false, "11:00", "22:00", false, null)),
                        breakTimes = mapOf("mon" to listOf(BusinessHoursBreakTime("15:00", "17:00"))),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )
        val filter = RamenShopFilter(isOpenSelected = true)

        assertEquals(
            setOf("shop-1"),
            RamenShops(listOf(shop)).filterBy(filter, LocalDateTime(2026, 8, 10, 12, 0)).keys,
        )
        assertTrue(RamenShops(listOf(shop)).filterBy(filter, LocalDateTime(2026, 8, 10, 16, 0)).isEmpty())
        assertTrue(RamenShops(listOf(shop)).filterBy(filter, LocalDateTime(2026, 8, 10, 23, 0)).isEmpty())
    }

    @Test
    fun `전날 심야 영업은 자정 이후에도 영업중으로 판정한다`() {
        val shop =
            ramenShopFixture().copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("sun" to BusinessHoursDay(false, "20:00", "01:00", true, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )

        assertEquals(
            setOf("shop-1"),
            RamenShops(listOf(shop))
                .filterBy(
                    RamenShopFilter(isOpenSelected = true),
                    LocalDateTime(2026, 8, 10, 0, 30),
                ).keys,
        )
    }

    @Test
    fun `closed null 파싱 불가 영업시간은 영업중이 아니다`() {
        val shops =
            listOf(
                ramenShopFixture(id = "closed").copy(
                    businessHoursDetails =
                        BusinessHours(
                            weekly = mapOf("mon" to BusinessHoursDay(true, "11:00", "22:00", false, null)),
                            breakTimes = emptyMap(),
                            lastOrders = emptyMap(),
                            notice = null,
                        ),
                ),
                ramenShopFixture(id = "null").copy(businessHoursDetails = null),
                ramenShopFixture(id = "invalid").copy(
                    businessHoursDetails =
                        BusinessHours(
                            weekly = mapOf("mon" to BusinessHoursDay(false, "bad", "22:00", false, null)),
                            breakTimes = emptyMap(),
                            lastOrders = emptyMap(),
                            notice = null,
                        ),
                ),
            )

        assertTrue(
            RamenShops(shops)
                .filterBy(
                    RamenShopFilter(isOpenSelected = true),
                    LocalDateTime(2026, 8, 10, 12, 0),
                ).isEmpty(),
        )
    }

    @Test
    fun `영업중과 카테고리 필터는 AND로 적용한다`() {
        val shop =
            ramenShopFixture(menuCategories = listOf(Category.JIRO)).copy(
                businessHoursDetails =
                    BusinessHours(
                        weekly = mapOf("mon" to BusinessHoursDay(false, "20:00", "01:00", true, null)),
                        breakTimes = emptyMap(),
                        lastOrders = emptyMap(),
                        notice = null,
                    ),
            )

        val result =
            RamenShops(listOf(shop)).filterBy(
                RamenShopFilter(setOf(Category.JIRO), isOpenSelected = true),
                LocalDateTime(2026, 8, 10, 20, 30),
            )

        assertEquals(setOf("shop-1"), result.keys)
    }

    @Test
    fun `전달한 가게 ID에 해당하는 가게만 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to ramenShopFixture(id = "1"),
                    "2" to ramenShopFixture(id = "2"),
                    "3" to ramenShopFixture(id = "3"),
                ),
            )

        // when
        val result = shops.filterByShopIds(setOf("1", "3"))

        // then
        assertEquals(setOf("1", "3"), result.keys)
    }

    @Test
    fun `전달한 가게 ID가 없으면 빈 RamenShops를 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to ramenShopFixture(id = "1"),
                    "2" to ramenShopFixture(id = "2"),
                ),
            )

        // when
        val result = shops.filterByShopIds(emptySet())

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `숨김 처리된 가게는 제외한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to ramenShopFixture(id = "1", name = "보이는 가게"),
                    "2" to ramenShopFixture(id = "2", name = "숨김 가게"),
                    "3" to ramenShopFixture(id = "3", name = "보이는 가게 2"),
                ),
            )

        // when
        val result = shops.filterNotHidden(setOf("2"))

        // then
        assertEquals(setOf("1", "3"), result.keys)
    }

    @Test
    fun `숨김 처리된 가게 ID가 없으면 모든 가게를 반환한다`() {
        // given
        val shops =
            RamenShops(
                mapOf(
                    "1" to ramenShopFixture(id = "1"),
                    "2" to ramenShopFixture(id = "2"),
                ),
            )

        // when
        val result = shops.filterNotHidden(emptySet())

        // then
        assertEquals(shops, result)
    }
}
