package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.model.OperatingNoticeResponse
import com.peto.ramap.fake.FakeRamenShopDataSource
import com.peto.ramap.fixture.ramenShopResponseFixture
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class DefaultOperatingNoticeRepositoryTest {
    @Test
    fun `서울 기준 오늘을 data source에 전달한다`() =
        runTest {
            val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))
            var requestedDate = today.minus(1, DateTimeUnit.DAY)
            val repository =
                DefaultOperatingNoticeRepository(
                    operatingNoticeDataSource =
                        object : OperatingNoticeDataSource {
                            override suspend fun fetchApprovedOperatingNotices(today: LocalDate) =
                                listOf(
                                    response(id = "current", endDate = today.toString()),
                                    response(
                                        id = "upcoming",
                                        startDate = today.plus(1, DateTimeUnit.DAY).toString(),
                                        endDate = today.plus(2, DateTimeUnit.DAY).toString(),
                                    ),
                                ).also { requestedDate = today }

                            override suspend fun fetchApprovedShopOperatingNotices(
                                shopId: String,
                                today: LocalDate,
                            ) = emptyList<OperatingNoticeResponse>()
                        },
                    ramenShopDataSource =
                        FakeRamenShopDataSource(
                            fetchByIdsResponses = listOf(ramenShopResponseFixture(id = "shop")),
                        ),
                )

            val notices = repository.fetchCurrentOperatingNotices().getOrThrow()

            assertEquals(today, requestedDate)
            assertEquals(listOf("current", "upcoming"), notices.map { it.id })
        }

    private fun response(
        id: String,
        endDate: String,
        startDate: String = endDate,
    ) = OperatingNoticeResponse(
        id = id,
        shopId = "shop",
        noticeType = "full_close",
        description = "내부 사정으로 쉽니다.",
        startDate = startDate,
        endDate = endDate,
        startTime = null,
        endTime = null,
        sourceUrl = null,
    )
}
