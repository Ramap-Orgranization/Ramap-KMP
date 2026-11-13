package com.peto.ramap.ui.main.map.contract

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.Category
import com.peto.ramap.domain.model.shop.RamenShopFilter
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.domain.model.shop.SearchQuery
import com.peto.ramap.domain.model.shop.ShopHoursFilter
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.map.model.search.SearchUiModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapHoursFilterTest {
    @Test
    fun `방문 날짜의 임시 휴무와 단축 영업 및 라스트오더를 지도와 목록에 함께 적용한다`() {
        val visitTime = LocalDateTime(2026, 11, 14, 19, 0)
        val hours =
            BusinessHours(
                weekly = mapOf("sat" to BusinessHoursDay(false, "17:00", "22:00", false, null)),
                breakTimes = emptyMap(),
                lastOrders = emptyMap(),
                notice = null,
            )
        val available =
            ramenShopFixture(id = "available", menuCategories = listOf(Category.JIRO))
                .copy(businessHoursDetails = hours, isRecommended = true)
        val temporarilyClosed = available.copy(id = "closed")
        val closesEarly = available.copy(id = "early")
        val lastOrder = available.copy(id = "last-order", businessHoursDetails = hours.copy(lastOrders = mapOf("sat" to listOf("19:00"))))
        val noHours = available.copy(id = "unknown", businessHoursDetails = null)
        val shops = RamenShops(listOf(available, temporarilyClosed, closesEarly, lastOrder, noHours))
        val temporaryClosure =
            OperatingNotice(
                id = "closure",
                shop = temporarilyClosed,
                type = OperatingNoticeType.TEMPORARY_CLOSURE,
                description = "",
                startDate = visitTime.date,
                endDate = visitTime.date,
                startTime = null,
                endTime = null,
                sourceUrl = null,
            )
        val state =
            MapUiState(
                currentDateTime = LocalDateTime(2026, 11, 13, 12, 0),
                shops = shops,
                filters = RamenShopFilter(setOf(Category.JIRO), hours = ShopHoursFilter.VisitTime(visitTime)),
                operatingNotices =
                    listOf(
                        temporaryClosure,
                        temporaryClosure.copy(
                            id = "early-closing",
                            shop = closesEarly,
                            type = OperatingNoticeType.PLANNED_CLOSING,
                            endTime = LocalTime(19, 0),
                        ),
                    ),
            )
        val searchState = state.copy(search = SearchUiModel(input = "라멘").updateResults(SearchQuery("라멘"), shops))
        val recommendedState = state.copy(isRecommendationEnabled = true, recommendedShops = shops)

        assertEquals(setOf(available.id), state.markerShops.keys)
        assertEquals(setOf(available.id), searchState.copy(selectedShop = temporarilyClosed).markerShops.keys)
        assertEquals(setOf(available.id), searchState.searchResultShops.keys)
        assertEquals(listOf(available), recommendedState.recommendationListShops)
        assertTrue(searchState.showSearchResults)
        assertEquals(shops.keys, state.copy(filters = state.filters.copy(hours = ShopHoursFilter.All)).markerShops.keys)
    }
}
