package com.peto.ramap.data.repository

import com.peto.ramap.coroutinesTest
import com.peto.ramap.data.model.PersonalizationResponse
import com.peto.ramap.fake.FakeHiddenShopDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultHiddenShopRepositoryTest {
    @Test
    fun `숨김 처리된 가게 ID 목록을 조회한다`() =
        coroutinesTest {
            // given
            val dataSource =
                FakeHiddenShopDataSource(
                    initialHiddenShops =
                        listOf(
                            PersonalizationResponse(shopId = "1"),
                            PersonalizationResponse(shopId = "2"),
                        ),
                )
            val repository = DefaultHiddenShopRepository(dataSource)

            // when
            val result = repository.fetchHiddenShopIds()

            // then
            assertEquals(setOf("1", "2"), result)
        }

    @Test
    fun `가게를 숨김 처리한다`() =
        coroutinesTest {
            // given
            val dataSource = FakeHiddenShopDataSource()
            val repository = DefaultHiddenShopRepository(dataSource)

            // when
            repository.hideShop("1")

            // then
            assertEquals(setOf("1"), repository.fetchHiddenShopIds())
        }

    @Test
    fun `숨김 처리된 가게를 해제한다`() =
        coroutinesTest {
            // given
            val dataSource =
                FakeHiddenShopDataSource(
                    initialHiddenShops =
                        listOf(
                            PersonalizationResponse(shopId = "1"),
                            PersonalizationResponse(shopId = "2"),
                        ),
                )
            val repository = DefaultHiddenShopRepository(dataSource)

            // when
            repository.unhideShop("1")

            // then
            assertEquals(setOf("2"), repository.fetchHiddenShopIds())
        }
}
