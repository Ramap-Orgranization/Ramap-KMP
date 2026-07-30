package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.domain.model.shop.AdministrativeArea
import com.peto.ramap.domain.model.shop.AdministrativeDistrict
import com.peto.ramap.domain.model.shop.AdministrativeDistricts
import com.peto.ramap.fake.FakeShopRankingDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DefaultShopRankingRepositoryTest {
    @Test
    fun `같은 지역의 시군구를 두 번 조회하면 데이터 소스는 한 번만 호출한다`() =
        runTest {
            val districts = districts("강남구", "마포구")
            val dataSource = FakeShopRankingDataSource(mapOf(AdministrativeArea.SEOUL.name to districts))
            val repository = DefaultShopRankingRepository(dataSource)

            val first = repository.fetchAdministrativeDistricts(AdministrativeArea.SEOUL).getOrThrow()
            val second = repository.fetchAdministrativeDistricts(AdministrativeArea.SEOUL).getOrThrow()

            assertEquals(districts, first)
            assertEquals(districts, second)
            assertEquals(1, dataSource.administrativeDistrictRequests.size)
        }

    @Test
    fun `서로 다른 지역의 시군구는 각각 데이터 소스를 호출한다`() =
        runTest {
            val seoulDistricts = districts("강남구")
            val gyeonggiDistricts = districts("수원시")
            val dataSource =
                FakeShopRankingDataSource(
                    mapOf(
                        AdministrativeArea.SEOUL.name to seoulDistricts,
                        AdministrativeArea.GYEONGGI.name to gyeonggiDistricts,
                    ),
                )
            val repository = DefaultShopRankingRepository(dataSource)

            assertEquals(
                seoulDistricts,
                repository.fetchAdministrativeDistricts(AdministrativeArea.SEOUL).getOrThrow(),
            )
            assertEquals(
                gyeonggiDistricts,
                repository.fetchAdministrativeDistricts(AdministrativeArea.GYEONGGI).getOrThrow(),
            )
            assertEquals(
                listOf(AdministrativeArea.SEOUL.name, AdministrativeArea.GYEONGGI.name),
                dataSource.administrativeDistrictRequests.map { it.area },
            )
        }

    @Test
    fun `시군구 조회 실패는 캐시하지 않아 다음 요청에서 다시 조회한다`() =
        runTest {
            val districts = districts("강남구")
            val dataSource =
                FakeShopRankingDataSource(
                    administrativeDistrictsByArea = mapOf(AdministrativeArea.SEOUL.name to districts),
                    failuresBeforeSuccess = mutableMapOf(AdministrativeArea.SEOUL.name to 1),
                )
            val repository = DefaultShopRankingRepository(dataSource)

            assertIs<RamapResult.Error>(
                repository.fetchAdministrativeDistricts(AdministrativeArea.SEOUL),
            )
            assertEquals(
                districts,
                repository.fetchAdministrativeDistricts(AdministrativeArea.SEOUL).getOrThrow(),
            )
            assertEquals(2, dataSource.administrativeDistrictRequests.size)
        }

    private fun districts(vararg names: String): AdministrativeDistricts = AdministrativeDistricts(names.map(::AdministrativeDistrict))
}
