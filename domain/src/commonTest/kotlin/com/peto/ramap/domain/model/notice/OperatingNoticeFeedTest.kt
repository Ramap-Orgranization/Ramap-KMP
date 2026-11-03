package com.peto.ramap.domain.model.notice

import com.peto.ramap.domain.model.businesshour.BusinessHours
import com.peto.ramap.domain.model.businesshour.BusinessHoursDay
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
    fun `오늘 매장도 미래 공지를 예정에 표시하고 검색과 저장 조건을 모두 적용한다`() {
        val today = notice("today")
        val future = notice("future").copy(startDate = LocalDate(2026, 10, 19), endDate = null)
        val other = future.copy(id = "other", shop = future.shop.copy(id = "other", name = "다른 매장"))
        val feed = feed(today, future, other)
        assertEquals(listOf("future", "other"), feed.representatives(true).map { it.id })
        assertEquals(1, feed.representatives(false, "  RAMEN ", setOf("shop")).size)
        assertEquals(0, feed.representatives(false, "ramen", emptySet()).size)
    }

    @Test
    fun `기간 휴무는 시작일부터 마지막 날까지 오늘에만 표시하고 종료되면 제외한다`() {
        val closure =
            notice("period", OperatingNoticeType.TEMPORARY_CLOSURE).copy(
                startDate = LocalDate(2026, 10, 10),
                endDate = LocalDate(2026, 10, 19),
            )
        val feed = feed(closure)
        val beforeClosure = feed.copy(referenceDate = LocalDate(2026, 10, 9))
        assertEquals(emptyList(), beforeClosure.representatives(false))
        assertEquals(listOf(closure), beforeClosure.representatives(true))
        assertEquals(listOf(closure), beforeClosure.scheduledNoticesForShop("shop"))

        for (currentDate in listOf(closure.startDate, date, LocalDate(2026, 10, 19))) {
            val ongoing = feed.copy(referenceDate = currentDate)
            assertEquals(listOf(closure), ongoing.representatives(false))
            assertEquals(emptyMap(), ongoing.scheduledGroups())
            assertEquals(emptyList(), ongoing.representatives(true))
            assertEquals(emptyList(), ongoing.scheduledNoticesForShop("shop"))
        }

        val afterClosure = feed.copy(referenceDate = LocalDate(2026, 10, 20))
        assertEquals(emptyList(), afterClosure.representatives(false))
        assertEquals(emptyMap(), afterClosure.scheduledGroups())
    }

    @Test
    fun `종료일이 없는 공지도 시작 전에는 예정에 표시하고 시작일부터 오늘에만 표시한다`() {
        val closure = notice("indefinite", OperatingNoticeType.TEMPORARY_CLOSURE).copy(endDate = null)
        val beforeClosure = feed(closure).copy(referenceDate = LocalDate(2026, 10, 17))
        assertEquals(listOf(closure), beforeClosure.representatives(true))
        assertEquals(emptyList(), beforeClosure.representatives(false))

        for (currentDate in listOf(date, LocalDate(2026, 10, 19))) {
            val ongoing = feed(closure).copy(referenceDate = currentDate)
            assertEquals(listOf(closure), ongoing.representatives(false))
            assertEquals(emptyList(), ongoing.representatives(true))
            assertEquals(emptyList(), ongoing.scheduledNoticesForShop("shop"))
        }
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
    fun `예정 기간은 연말을 넘어 정렬하고 시작일에는 오늘 목록으로 이동한다`() {
        val december = notice("december").copy(startDate = LocalDate(2026, 12, 31), endDate = LocalDate(2027, 1, 2))
        val january = december.copy(id = "january", shop = december.shop.copy(id = "other"), startDate = LocalDate(2027, 1, 1), endDate = null)
        assertEquals(
            listOf(LocalDate(2026, 12, 31), LocalDate(2027, 1, 1), LocalDate(2027, 1, 2)),
            feed(january, december).scheduledGroups().keys.toList(),
        )
        assertEquals(
            listOf(LocalDate(2027, 1, 1)),
            feed(january, december)
                .copy(referenceDate = LocalDate(2026, 12, 31))
                .scheduledGroups()
                .keys
                .toList(),
        )
        assertEquals(
            listOf(december),
            feed(january, december).copy(referenceDate = december.startDate).representatives(false),
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
        val feed = feed(calendar, closing).copy(referenceDate = LocalDate(2026, 9, 30))
        assertEquals(emptyList(), feed.representatives(false))
        assertEquals(listOf(date, LocalDate(2026, 10, 21), LocalDate(2026, 10, 23)), feed.scheduledGroups().keys.toList())
        assertEquals(listOf(calendar), feed.scheduledGroups().getValue(LocalDate(2026, 10, 21)))
        assertEquals(listOf(calendar, closing), feed.noticesForShopOn("shop", LocalDate(2026, 10, 21)))
        assertEquals(1, feed.representatives(true).size)

        val ongoing = feed.copy(referenceDate = date)
        assertEquals(listOf(calendar), ongoing.representatives(false))
        assertEquals(mapOf(LocalDate(2026, 10, 21) to listOf(closing)), ongoing.scheduledGroups())
        assertEquals(listOf(closing), ongoing.scheduledNoticesForShop("shop"))
    }

    @Test
    fun `진행 중인 휴무는 예정 대표와 상세에서 제외하고 별도의 미래 공지만 표시한다`() {
        val closure = notice("period", OperatingNoticeType.TEMPORARY_CLOSURE).copy(endDate = LocalDate(2026, 10, 23))
        val future = notice("early").copy(startDate = LocalDate(2026, 10, 19), endDate = LocalDate(2026, 10, 19))
        val unrelated = closure.copy(id = "unrelated", shop = closure.shop.copy(id = "other"))
        val feed = feed(closure, future, unrelated)
        val groups = feed.scheduledGroups()
        assertEquals(mapOf(LocalDate(2026, 10, 19) to listOf(future)), groups)
        assertEquals(setOf(closure, unrelated), feed.representatives(false).toSet())
        assertEquals(listOf(future), feed.representatives(true))
        assertEquals(listOf(future), feed.scheduledNoticesForShop("shop"))
        assertEquals(listOf(closure, future), feed.noticesForShopOn("shop", LocalDate(2026, 10, 19)))
    }

    @Test
    fun `예정 상세는 대표에 가려진 원본도 포함하고 지난 공지와 중복은 제외한다`() {
        val futureDate = LocalDate(2026, 10, 19)
        val closure = notice("closure", OperatingNoticeType.TEMPORARY_CLOSURE).copy(startDate = futureDate, endDate = futureDate)
        val planned = notice("planned", OperatingNoticeType.PLANNED_CLOSING).copy(startDate = futureDate, endDate = futureDate)
        val other = planned.copy(id = "other", shop = planned.shop.copy(id = "other"))
        val feed = feed(notice("today"), closure, planned, closure, other)
        assertEquals(listOf("closure"), feed.representatives(true, bookmarkedShopIds = setOf("shop")).map { it.id })
        assertEquals(listOf("closure", "planned"), feed.scheduledNoticesForShop("shop").map { it.id })
    }

    private fun feed(vararg notices: OperatingNotice) = OperatingNoticeFeed(date, notices.map { OperatingNoticeFeedItem(it) })

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
