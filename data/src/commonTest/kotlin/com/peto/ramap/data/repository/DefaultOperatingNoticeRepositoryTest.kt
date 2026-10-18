package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.model.OperatingNoticeFeedItemResponse
import com.peto.ramap.data.model.OperatingNoticeFeedResponse
import com.peto.ramap.data.model.OperatingNoticeResponse
import com.peto.ramap.data.model.OperatingNoticeShopResponse
import com.peto.ramap.domain.model.notice.OperatingNoticeType
import com.peto.ramap.domain.model.shop.AdministrativeArea
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
                            override suspend fun fetchOperatingNoticeFeed(today: LocalDate) =
                                com.peto.ramap.data.model
                                    .OperatingNoticeFeedResponse(today.toString(), emptyList())

                            override suspend fun fetchApprovedOperatingNotices(today: LocalDate) =
                                listOf(
                                    response(id = "current", endDate = today.toString()),
                                    response(id = "removed", endDate = today.toString()).copy(noticeType = "operating_notice"),
                                    response(id = "missing-time", endDate = today.toString()).copy(noticeType = "early_close"),
                                    response(
                                        id = "expired",
                                        startDate = today.minus(2, DateTimeUnit.DAY).toString(),
                                        endDate = today.minus(1, DateTimeUnit.DAY).toString(),
                                    ),
                                    response(
                                        id = "upcoming",
                                        startDate = today.plus(1, DateTimeUnit.DAY).toString(),
                                        endDate = today.plus(2, DateTimeUnit.DAY).toString(),
                                    ),
                                ).also { requestedDate = today }

                            override suspend fun fetchApprovedShopOperatingNotices(
                                shopId: String,
                                earliestDate: LocalDate,
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

    @Test
    fun `목록 조회는 서울 기준일과 canonical 유형 및 정규화 지역을 전달한다`() =
        runTest {
            val today = Clock.System.todayIn(TimeZone.of("Asia/Seoul"))
            var requestedDate: LocalDate? = null
            val source =
                object : OperatingNoticeDataSource {
                    override suspend fun fetchOperatingNoticeFeed(today: LocalDate): OperatingNoticeFeedResponse {
                        requestedDate = today
                        return OperatingNoticeFeedResponse(
                            today.toString(),
                            listOf(
                                OperatingNoticeFeedItemResponse(
                                    notice = response("planned", today.toString()).copy(noticeType = "early_close", noticeCategory = "planned_close", endTime = "16:00:00"),
                                    shop = OperatingNoticeShopResponse(id = "shop", name = "매장", lat = 37.0, lng = 127.0, createdAt = "", updatedAt = ""),
                                    area = "SEOUL",
                                    sigungu = "마포구",
                                ),
                            ),
                        )
                    }

                    override suspend fun fetchApprovedOperatingNotices(today: LocalDate) = error("기존 경로 호출")

                    override suspend fun fetchApprovedShopOperatingNotices(
                        shopId: String,
                        earliestDate: LocalDate,
                    ) = error("기존 경로 호출")
                }
            val feed = DefaultOperatingNoticeRepository(source, FakeRamenShopDataSource()).fetchOperatingNoticeFeed().getOrThrow()
            assertEquals(today, requestedDate)
            assertEquals(today, feed.referenceDate)
            assertEquals(
                OperatingNoticeType.PLANNED_CLOSING,
                feed.items
                    .single()
                    .notice.type,
            )
            assertEquals(AdministrativeArea.SEOUL, feed.items.single().area)
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
