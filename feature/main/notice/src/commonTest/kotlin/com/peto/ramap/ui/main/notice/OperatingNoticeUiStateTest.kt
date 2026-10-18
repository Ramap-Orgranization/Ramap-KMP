package com.peto.ramap.ui.main.notice

import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.notice.contract.OperatingNoticeUiState
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OperatingNoticeUiStateTest {
    private val today = LocalDate(2026, 10, 18)
    private val next = LocalDate(2026, 10, 21)
    private val later = LocalDate(2026, 10, 23)

    @Test
    fun `같은 매장의 오늘과 각 예정 날짜를 표시하되 탭 숫자는 고유 매장 수다`() {
        val current = notice("today", today)
        val future = notice("future", next)
        val anotherDate = notice("later", later)
        val state = state(current, future, anotherDate)
        assertEquals(listOf(current), state.todayShops)
        assertEquals(listOf(next, later), state.scheduledGroups.keys.toList())
        assertEquals(listOf(future, anotherDate), state.scheduledGroups.values.flatten())
        assertEquals(1, state.scheduledShops.size)
        assertEquals(0, state.copy(query = "없는 매장").scheduledGroups.size)
    }

    @Test
    fun `진행 중인 기간 휴무는 일정의 전체 날짜와 각 날짜 상세에 포함하고 매장 수는 한 번 센다`() {
        val tomorrow = LocalDate(2026, 10, 19)
        val closure = notice("period", LocalDate(2026, 10, 10)).copy(endDate = tomorrow)
        val state = state(closure)
        val expectedDates = (10..19).map { LocalDate(2026, 10, it) }
        assertEquals(listOf(closure), state.todayShops)
        assertEquals(expectedDates.associateWith { listOf(closure) }, state.scheduledGroups)
        assertEquals(1, state.todayCount)
        assertEquals(1, state.scheduledCount)
        for (date in expectedDates) {
            assertEquals(listOf(closure), state.selectNotice(closure, date).notices)
        }
    }

    @Test
    fun `상세 선택은 선택 날짜에 적용되는 같은 매장 공지만 스냅샷으로 보존한다`() {
        val current = notice("today", today)
        val duplicate = notice("same-day", today)
        val future = notice("future", next)
        val anotherDate = notice("later", later)
        val state = state(current, duplicate, future, anotherDate)
        val selectedToday = state.selectNotice(current, today)
        val selectedFuture = state.selectNotice(future, next)
        assertEquals(setOf("today", "same-day"), selectedToday.notices.map { it.id }.toSet())
        assertEquals(listOf(future), selectedFuture.notices)
        assertEquals(next, selectedFuture.date)
        val changed = state.copy(query = "다른", scheduledSelected = false, feed = OperatingNoticeFeed(today, emptyList()))
        assertEquals(emptyList(), changed.todayShops)
        assertEquals(listOf(future), selectedFuture.notices)
    }

    @Test
    fun `feed 없는 분리 목록도 실제 날짜 기준으로 오늘과 예정 및 상세를 구분한다`() {
        val current = notice("today", today)
        val future = notice("future", next)
        val anotherDate = notice("later", later)
        val state =
            OperatingNoticeUiState(
                todayOperatingNotices = listOf(current),
                scheduledOperatingNotices = listOf(future, anotherDate),
                fallbackReferenceDate = today,
            )
        assertEquals(listOf(current), state.todayShops)
        assertEquals(listOf(next, later), state.scheduledGroups.keys.toList())
        assertEquals(1, state.scheduledShops.size)
        assertEquals(listOf(future), state.selectNotice(future, next).notices)
    }

    @Test
    fun `저장 필터가 켜져 있지 않으면 개수를 표시하고 켜져 있을 때는 개인화 로딩 상태에 따라 개수 표시 여부를 결정한다`() {
        val current = notice("today", today)
        val future = notice("future", next)
        val baseState = state(current, future)

        val defaultState = baseState.copy(savedOnly = false, personalizationState = PersonalizationBootstrapState.Loading)
        assertTrue(defaultState.showCounts)
        assertEquals(1, defaultState.todayCount)
        assertEquals(1, defaultState.scheduledCount)

        val savedLoadingState = baseState.copy(savedOnly = true, personalizationState = PersonalizationBootstrapState.Loading)
        assertFalse(savedLoadingState.showCounts)
        assertNull(savedLoadingState.todayCount)
        assertNull(savedLoadingState.scheduledCount)

        val savedSuccessState = baseState.copy(savedOnly = true, personalizationState = PersonalizationBootstrapState.Success(Personalization()))
        assertTrue(savedSuccessState.showCounts)
    }

    private fun state(vararg notices: OperatingNotice) =
        OperatingNoticeUiState(
            feed = OperatingNoticeFeed(today, notices.map { OperatingNoticeFeedItem(it) }),
        )

    private fun notice(
        id: String,
        date: LocalDate,
    ) = OperatingNotice(
        id = id,
        shop = ramenShopFixture(id = "shop"),
        type = OperatingNoticeType.TEMPORARY_CLOSURE,
        description = "운영 일정",
        startDate = date,
        endDate = date,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )
}
