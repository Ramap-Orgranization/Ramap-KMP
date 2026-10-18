package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AreaFilter
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class OperatingNoticeFeedTest {
    private val date = LocalDate(2026, 10, 18)

    @Test
    fun `같은 매장의 실제 마감과 예정 마감 중 빠른 시각을 선택하고 휴무가 우선한다`() {
        val planned = notice("planned", type = OperatingNoticeType.PLANNED_CLOSING, hour = 16)
        val early = notice("early", hour = 14)
        assertEquals(early, feed(planned, early).representatives(false).single())
        assertEquals(early, feed(planned.copy(endTime = LocalTime(14, 0)), early).representatives(false).single())
        val closed = notice("closed", type = OperatingNoticeType.TEMPORARY_CLOSURE)
        assertEquals(closed, feed(planned, early, closed).representatives(false).single())
    }

    @Test
    fun `오늘 매장도 미래 공지를 예정에 표시하고 검색 지역 저장 조건을 모두 적용한다`() {
        val today = notice("today")
        val future = notice("future").copy(startDate = LocalDate(2026, 10, 19), endDate = null)
        val other = future.copy(id = "other", shop = future.shop.copy(id = "other", name = "다른 매장"))
        val feed = feed(today, future, other)
        assertEquals(listOf("future", "other"), feed.representatives(true).map { it.id })
        assertEquals(1, feed.representatives(false, "  RAMEN ", AreaFilter.Province(AdministrativeArea.SEOUL), setOf("shop")).size)
        assertEquals(0, feed.representatives(false, "ramen", AreaFilter.Province(AdministrativeArea.BUSAN), setOf("shop")).size)
        assertEquals(listOf("마포구"), feed.districts(AdministrativeArea.SEOUL))
    }

    @Test
    fun `진행 중인 기간 휴무는 지난 날짜와 오늘을 포함해 시작일부터 종료일까지 일정에 표시한다`() {
        val closure =
            notice("period", OperatingNoticeType.TEMPORARY_CLOSURE).copy(
                startDate = LocalDate(2026, 10, 10),
                endDate = LocalDate(2026, 10, 19),
            )
        val feed = feed(closure)
        val expectedDates = (10..19).map { LocalDate(2026, 10, it) }
        assertEquals(listOf(closure), feed.representatives(false))
        assertEquals(expectedDates, feed.scheduledGroups().keys.toList())
        for (notices in feed.scheduledGroups().values) {
            assertEquals(listOf(closure), notices)
        }
        assertEquals(listOf(closure), feed.representatives(true))

        val lastDay = feed.copy(referenceDate = LocalDate(2026, 10, 19))
        assertEquals(listOf(closure), lastDay.representatives(false))
        assertEquals(expectedDates, lastDay.scheduledGroups().keys.toList())

        val afterClosure = feed.copy(referenceDate = LocalDate(2026, 10, 20))
        assertEquals(emptyList(), afterClosure.representatives(false))
        assertEquals(emptyMap(), afterClosure.scheduledGroups())
    }

    @Test
    fun `익일 마감은 보정 날짜로 비교하고 같은 종료 시각은 ID로 순서를 고정한다`() {
        val overnight =
            notice("late").shop.copy(
                businessHoursDetails = BusinessHours(mapOf("sun" to BusinessHoursDay(false, "18:00", "02:00", true, null)), emptyMap(), emptyMap(), null),
            )
        val midnight = notice("planned", OperatingNoticeType.PLANNED_CLOSING, 1).copy(shop = overnight)
        val evening = notice("actual", hour = 23).copy(shop = overnight)
        assertEquals(evening, feed(midnight, evening).representatives(false).single())
        assertEquals("a", feed(evening.copy(id = "z"), evening.copy(id = "a")).representatives(false).single().id)
    }

    @Test
    fun `예정은 기간에 포함된 날짜별로 매장을 반복하고 고유 매장 수를 유지한다`() {
        val first = notice("first").copy(startDate = LocalDate(2026, 10, 19), endDate = LocalDate(2026, 10, 21))
        val laterForSameShop = first.copy(id = "same-later", startDate = LocalDate(2026, 10, 23), endDate = null)
        val later = laterForSameShop.copy(id = "other", shop = first.shop.copy(id = "other", name = "Other"))
        val feed = feed(later, laterForSameShop, first)
        val groups = feed.scheduledGroups()
        assertEquals(
            listOf(LocalDate(2026, 10, 19), LocalDate(2026, 10, 20), LocalDate(2026, 10, 21), LocalDate(2026, 10, 23)),
            groups.keys.toList(),
        )
        assertEquals(listOf("first", "first", "first", "other", "same-later"), groups.values.flatten().map { it.id })
        assertEquals(2, feed.representatives(true).size)
        assertEquals(5, groups.values.sumOf { it.size })
        assertEquals(listOf(LocalDate(2026, 10, 23)), feed.scheduledGroups(query = "Other").keys.toList())
        assertEquals(emptyMap(), feed.scheduledGroups(bookmarkedShopIds = emptySet()))
    }

    @Test
    fun `예정 기간은 연말을 넘어 종료일까지 실제 날짜로 정렬한다`() {
        val december = notice("december").copy(startDate = LocalDate(2026, 12, 31), endDate = LocalDate(2027, 1, 2))
        val january = december.copy(id = "january", shop = december.shop.copy(id = "other"), startDate = LocalDate(2027, 1, 1), endDate = null)
        assertEquals(
            listOf(LocalDate(2026, 12, 31), LocalDate(2027, 1, 1), LocalDate(2027, 1, 2)),
            feed(january, december).scheduledGroups().keys.toList(),
        )
    }

    @Test
    fun `달력 공지는 명시한 미래 휴무일에만 반복하고 해당 날짜의 휴무가 대표로 우선한다`() {
        val calendar =
            notice("calendar", OperatingNoticeType.TEMPORARY_CLOSURE).copy(
                startDate = LocalDate(2026, 10, 1),
                endDate = LocalDate(2026, 10, 31),
                dailySchedules =
                    listOf(
                        OperatingNoticeDay(date, true),
                        OperatingNoticeDay(LocalDate(2026, 10, 20), false),
                        OperatingNoticeDay(LocalDate(2026, 10, 21), true),
                        OperatingNoticeDay(LocalDate(2026, 10, 23), true),
                    ),
            )
        val closing = notice("closing").copy(startDate = LocalDate(2026, 10, 21), endDate = LocalDate(2026, 10, 21))
        val feed = feed(calendar, closing)
        assertEquals(listOf(calendar), feed.representatives(false))
        assertEquals(listOf(LocalDate(2026, 10, 21), LocalDate(2026, 10, 23)), feed.scheduledGroups().keys.toList())
        assertEquals(listOf(calendar), feed.scheduledGroups().getValue(LocalDate(2026, 10, 21)))
        assertEquals(listOf(calendar, closing), feed.noticesForShopOn("shop", LocalDate(2026, 10, 21)))
        assertEquals(1, feed.representatives(true).size)
    }

    @Test
    fun `진행 중인 기간 휴무는 전체 날짜에 표시하고 겹치는 미래 마감 공지보다 우선한다`() {
        val closure = notice("period", OperatingNoticeType.TEMPORARY_CLOSURE).copy(endDate = LocalDate(2026, 10, 23))
        val future = notice("early").copy(startDate = LocalDate(2026, 10, 19), endDate = LocalDate(2026, 10, 19))
        val unrelated = closure.copy(id = "unrelated", shop = closure.shop.copy(id = "other"))
        val feed = feed(closure, future, unrelated)
        val groups = feed.scheduledGroups()
        assertEquals(
            listOf(LocalDate(2026, 10, 18), LocalDate(2026, 10, 19), LocalDate(2026, 10, 20), LocalDate(2026, 10, 21), LocalDate(2026, 10, 22), LocalDate(2026, 10, 23)),
            groups.keys.toList(),
        )
        for (notices in groups.values) {
            assertEquals(setOf(closure, unrelated), notices.toSet())
        }
        assertEquals(2, feed.representatives(true).size)
        assertEquals(listOf(closure, future), feed.noticesForShopOn("shop", LocalDate(2026, 10, 19)))
    }

    private fun feed(vararg notices: OperatingNotice) = OperatingNoticeFeed(date, notices.map { OperatingNoticeFeedItem(it, AdministrativeArea.SEOUL, "마포구") })

    private fun notice(
        id: String,
        type: OperatingNoticeType = OperatingNoticeType.EARLY_CLOSING,
        hour: Int = 14,
    ) = OperatingNotice(
        id = id,
        shop = ramenShopFixture(id = "shop", name = "Ramen"),
        type = type,
        description = "원문",
        startDate = date,
        endDate = date,
        startTime = null,
        endTime = if (type.hasClosingTime) LocalTime(hour, 0) else null,
        sourceUrl = null,
    )
}
