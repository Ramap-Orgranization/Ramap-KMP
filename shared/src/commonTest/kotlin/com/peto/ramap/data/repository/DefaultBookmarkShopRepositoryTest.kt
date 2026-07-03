package com.peto.ramap.data.repository

import com.peto.ramap.coroutinesTest
import com.peto.ramap.data.model.PersonalizationResponse
import com.peto.ramap.fake.FakeBookmarkShopDataSource
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultBookmarkShopRepositoryTest {
    @Test
    fun `북마크된 가게 ID 목록을 조회한다`() =
        coroutinesTest {
            // given
            val dataSource =
                FakeBookmarkShopDataSource(
                    initialBookmarks =
                        listOf(
                            PersonalizationResponse(shopId = "1"),
                            PersonalizationResponse(shopId = "2"),
                        ),
                )
            val repository = DefaultBookmarkShopRepository(dataSource)

            // when
            val result = repository.fetchBookmarkedShopIds()

            // then
            assertEquals(setOf("1", "2"), result)
        }

    @Test
    fun `북마크를 추가한다`() =
        coroutinesTest {
            // given
            val dataSource = FakeBookmarkShopDataSource()
            val repository = DefaultBookmarkShopRepository(dataSource)

            // when
            repository.addBookmark("1")

            // then
            assertEquals(setOf("1"), repository.fetchBookmarkedShopIds())
        }

    @Test
    fun `북마크를 제거한다`() =
        coroutinesTest {
            // given
            val dataSource =
                FakeBookmarkShopDataSource(
                    initialBookmarks =
                        listOf(
                            PersonalizationResponse(shopId = "1"),
                            PersonalizationResponse(shopId = "2"),
                        ),
                )
            val repository = DefaultBookmarkShopRepository(dataSource)

            // when
            repository.removeBookmark("1")

            // then
            assertEquals(setOf("2"), repository.fetchBookmarkedShopIds())
        }
}
