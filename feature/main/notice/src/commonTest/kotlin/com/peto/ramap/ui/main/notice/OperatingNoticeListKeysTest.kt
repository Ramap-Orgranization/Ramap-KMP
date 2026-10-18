package com.peto.ramap.ui.main.notice

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.notice.OperatingNoticeFeed
import com.peto.ramap.domain.model.notice.OperatingNoticeFeedItem
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperatingNoticeListKeysTest {
    @Test
    fun `같은 매장을 열흘간 표시해도 실제 목록 행의 키가 겹치지 않는다`() {
        val closure =
            OperatingNotice(
                id = "period",
                shop = ramenShopFixture(id = "shop"),
                type = OperatingNoticeType.TEMPORARY_CLOSURE,
                description = "기간 휴무",
                startDate = LocalDate(2026, 10, 10),
                endDate = LocalDate(2026, 10, 19),
                startTime = null,
                endTime = null,
                sourceUrl = null,
            )
        val feed = OperatingNoticeFeed(LocalDate(2026, 10, 18), listOf(OperatingNoticeFeedItem(closure)))
        val keys = mutableListOf<Any>()
        val scope =
            object : LazyListScope {
                override fun items(
                    count: Int,
                    key: ((Int) -> Any)?,
                    contentType: (Int) -> Any?,
                    itemContent: @Composable LazyItemScope.(Int) -> Unit,
                ) {
                    for (index in 0 until count) {
                        keys.add(requireNotNull(key)(index))
                    }
                }
            }
        for ((date, notices) in feed.scheduledGroups()) {
            addOperatingNoticeRows(
                scope = scope,
                notices = notices,
                date = date,
                onSelected = { _, _ -> },
            )
        }
        assertEquals(10, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { it is String })
    }
}
