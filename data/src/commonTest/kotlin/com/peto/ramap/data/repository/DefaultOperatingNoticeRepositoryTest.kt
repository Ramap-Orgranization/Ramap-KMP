package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.model.OperatingNoticeResponse
import com.peto.ramap.domain.model.operatingnotice.OperatingNoticeStatus
import com.peto.ramap.fake.FakeRamenShopDataSource
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class DefaultOperatingNoticeRepositoryTest {
    @Test
    fun `서울 기준으로 종료된 영업 변동은 제외한다`() =
        runTest {
            val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))
            val repository =
                DefaultOperatingNoticeRepository(
                    operatingNoticeDataSource =
                        object : OperatingNoticeDataSource {
                            override suspend fun fetchApprovedOperatingNotices() =
                                listOf(
                                    response(id = "ended", endDate = today.minus(1, DateTimeUnit.DAY).toString()),
                                    response(id = "current", endDate = today.toString()),
                                    response(
                                        id = "upcoming",
                                        startDate = today.plus(1, DateTimeUnit.DAY).toString(),
                                        endDate = today.plus(2, DateTimeUnit.DAY).toString(),
                                    ),
                                )
                        },
                    ramenShopDataSource =
                        FakeRamenShopDataSource(
                            fetchByIdsResponses = listOf(ramenShopResponseFixture(id = "shop")),
                        ),
                )

            val notices = repository.fetchCurrentOperatingNotices().getOrThrow()

            assertEquals(listOf("current", "upcoming"), notices.map { it.id })
            assertEquals(OperatingNoticeStatus.ONGOING, notices[0].statusOn(today))
            assertEquals(OperatingNoticeStatus.UPCOMING, notices[1].statusOn(today))
        }

    private fun response(
        id: String,
        endDate: String,
        startDate: String = endDate,
    ) = OperatingNoticeResponse(
        id = id,
        shopId = "shop",
        noticeType = "full_close",
        title = "임시 휴무",
        description = "내부 사정으로 쉽니다.",
        startDate = startDate,
        endDate = endDate,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )
}
