package com.peto.ramap.data.repository

import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.PersonalizationResponse
import com.peto.ramap.fake.FakeBookmarkShopDataSource
import com.peto.ramap.fake.FakeHiddenShopDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultPersonalizationRepositoryTest {
    @Test
    fun `북마크된 매장을 숨길 때 원자적 숨김 데이터소스를 호출한다`() =
        runTest {
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(),
                    hiddenShopDataSource,
                )

            repository.hideShop("shop-1", removeBookmark = true).getOrThrow()

            assertEquals("shop-1", hiddenShopDataSource.hiddenBookmarkedShopId)
        }

    @Test
    fun `북마크를 유지할 때 일반 숨김 데이터소스를 호출한다`() =
        runTest {
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(),
                    hiddenShopDataSource,
                )

            repository.hideShop("shop-1", removeBookmark = false).getOrThrow()

            assertNull(hiddenShopDataSource.hiddenBookmarkedShopId)
            assertEquals(
                listOf(PersonalizationResponse("shop-1")),
                hiddenShopDataSource.fetchHiddenShopIds(),
            )
        }
}
