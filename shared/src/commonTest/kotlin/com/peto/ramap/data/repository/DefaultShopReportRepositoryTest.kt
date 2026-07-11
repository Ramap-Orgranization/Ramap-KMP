package com.peto.ramap.data.repository

import com.peto.ramap.data.model.ShopInformationReportRequest
import com.peto.ramap.data.model.UnregisteredPlaceReportRequest
import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.domain.model.ShopInformationReport
import com.peto.ramap.domain.model.UnregisteredPlaceReport
import com.peto.ramap.fake.FakeShopReportDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultShopReportRepositoryTest {
    @Test
    fun `매장 정보 제보를 요청 모델로 변환해 저장한다`() =
        runTest {
            val dataSource = FakeShopReportDataSource()
            val repository = DefaultShopReportRepository(dataSource)
            val report =
                ShopInformationReport(
                    shopId = "shop-1",
                    shopName = "라멘집",
                    wrongFields = setOf(ShopInformationField.ADDRESS, ShopInformationField.OTHER),
                    description = "주소가 달라요",
                )

            repository.submit(report)

            assertEquals(
                ShopInformationReportRequest(
                    shopId = "shop-1",
                    shopName = "라멘집",
                    wrongFields = listOf("address", "other"),
                    description = "주소가 달라요",
                ),
                dataSource.insertedReport,
            )
        }

    @Test
    fun `미등록 장소 제보를 요청 모델로 변환해 저장한다`() =
        runTest {
            val dataSource = FakeShopReportDataSource()
            val repository = DefaultShopReportRepository(dataSource)

            repository.submit(UnregisteredPlaceReport(placeUrl = "https://map.naver.com/p/entry/place/123"))

            assertEquals(
                UnregisteredPlaceReportRequest(placeUrl = "https://map.naver.com/p/entry/place/123"),
                dataSource.insertedPlaceReport,
            )
        }
}
