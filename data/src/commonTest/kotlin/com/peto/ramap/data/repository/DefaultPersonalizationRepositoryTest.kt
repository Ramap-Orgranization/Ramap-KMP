package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.PersonalizationResponse
import com.peto.ramap.fake.FakeBookmarkShopDataSource
import com.peto.ramap.fake.FakeHiddenShopDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultPersonalizationRepositoryTest {
    @Test
    fun `개인화 조회 성공시 조회한 북마크 아이디를 공유 상태에 반영한다`() =
        runTest {
            val bookmark = PersonalizationResponse("bookmarked-shop")
            val hiddenShop = PersonalizationResponse("hidden-shop")
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    FakeHiddenShopDataSource(listOf(hiddenShop)),
                )

            val personalization = repository.fetchPersonalization().getOrThrow()

            assertEquals(setOf(bookmark.shopId), repository.bookmarkedShopIds.value)
            assertEquals(setOf(hiddenShop.shopId), personalization.hiddenShopIds)
        }

    @Test
    fun `개인화 조회 실패시 기존 북마크 공유 상태를 유지한다`() =
        runTest {
            val bookmark = PersonalizationResponse("bookmarked-shop")
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    hiddenShopDataSource,
                )
            repository.fetchPersonalization().getOrThrow()
            hiddenShopDataSource.error = IllegalStateException("failure")

            val result = repository.fetchPersonalization()

            assertIs<RamapResult.Error>(result)
            assertEquals(setOf(bookmark.shopId), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크 추가 성공시 공유 상태에 매장 아이디를 추가한다`() =
        runTest {
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(),
                    FakeHiddenShopDataSource(),
                )

            repository.addBookmark("shop-1").getOrThrow()

            assertEquals(setOf("shop-1"), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크 추가 실패시 공유 상태를 변경하지 않는다`() =
        runTest {
            val bookmarkDataSource = FakeBookmarkShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    bookmarkDataSource,
                    FakeHiddenShopDataSource(),
                )
            bookmarkDataSource.error = IllegalStateException("failure")

            val result = repository.addBookmark("shop-1")

            assertIs<RamapResult.Error>(result)
            assertEquals(emptySet(), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크 제거 성공시 공유 상태에서 매장 아이디를 제거한다`() =
        runTest {
            val bookmark = PersonalizationResponse("shop-1")
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    FakeHiddenShopDataSource(),
                )
            repository.fetchPersonalization().getOrThrow()

            repository.removeBookmark(bookmark.shopId).getOrThrow()

            assertEquals(emptySet(), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크 제거 실패시 공유 상태를 변경하지 않는다`() =
        runTest {
            val bookmark = PersonalizationResponse("shop-1")
            val bookmarkDataSource = FakeBookmarkShopDataSource(listOf(bookmark))
            val repository =
                DefaultPersonalizationRepository(
                    bookmarkDataSource,
                    FakeHiddenShopDataSource(),
                )
            repository.fetchPersonalization().getOrThrow()
            bookmarkDataSource.error = IllegalStateException("failure")

            val result = repository.removeBookmark(bookmark.shopId)

            assertIs<RamapResult.Error>(result)
            assertEquals(setOf(bookmark.shopId), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크된 매장을 숨길 때 원자적 숨김 데이터소스를 호출한다`() =
        runTest {
            val bookmark = PersonalizationResponse("shop-1")
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    hiddenShopDataSource,
                )
            repository.fetchPersonalization().getOrThrow()

            repository.hideShop("shop-1", removeBookmark = true).getOrThrow()

            assertEquals("shop-1", hiddenShopDataSource.hiddenBookmarkedShopId)
            assertEquals(emptySet(), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크된 매장 숨김 실패시 공유 상태를 변경하지 않는다`() =
        runTest {
            val bookmark = PersonalizationResponse("shop-1")
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    hiddenShopDataSource,
                )
            repository.fetchPersonalization().getOrThrow()
            hiddenShopDataSource.error = IllegalStateException("failure")

            val result = repository.hideShop(bookmark.shopId, removeBookmark = true)

            assertIs<RamapResult.Error>(result)
            assertEquals(setOf(bookmark.shopId), repository.bookmarkedShopIds.value)
        }

    @Test
    fun `북마크를 유지할 때 일반 숨김 데이터소스를 호출한다`() =
        runTest {
            val bookmark = PersonalizationResponse("shop-1")
            val hiddenShopDataSource = FakeHiddenShopDataSource()
            val repository =
                DefaultPersonalizationRepository(
                    FakeBookmarkShopDataSource(listOf(bookmark)),
                    hiddenShopDataSource,
                )
            repository.fetchPersonalization().getOrThrow()

            repository.hideShop("shop-1", removeBookmark = false).getOrThrow()

            assertNull(hiddenShopDataSource.hiddenBookmarkedShopId)
            assertEquals(setOf(bookmark.shopId), repository.bookmarkedShopIds.value)
            assertEquals(
                listOf(PersonalizationResponse("shop-1")),
                hiddenShopDataSource.fetchHiddenShopIds(),
            )
        }
}
