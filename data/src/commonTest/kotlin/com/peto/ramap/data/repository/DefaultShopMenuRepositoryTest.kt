package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.domain.model.menu.MenuSection
import com.peto.ramap.domain.model.menu.Menus
import com.peto.ramap.domain.usecase.ShopDetail
import com.peto.ramap.fake.FakeRamenShopRepository
import com.peto.ramap.fixture.ramenShopFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DefaultShopMenuRepositoryTest {
    @Test
    fun `기존 상세 조회의 메뉴 순서와 갱신일을 그대로 반환한다`() =
        runTest {
            val sections = listOf(MenuSection("event", "한정 메뉴", displayOrder = 0, items = Menus(emptyList())))
            val source = FakeRamenShopRepository(shopDetail = ShopDetail(ramenShopFixture(id = "shop"), 0L, null, null, null, menuSections = sections, menuUpdatedAt = "2026-10-14"))
            val menu = DefaultShopMenuRepository(source).fetchShopMenu("shop").getOrThrow()
            assertEquals(listOf("shop"), source.requestedShopDetailIds)
            assertSame(sections, menu.sections)
            assertEquals("2026-10-14", menu.updatedAt)
        }

    @Test
    fun `기존 조회 실패를 성공한 빈 메뉴로 바꾸지 않는다`() =
        runTest {
            val error = RamapError.Unknown(IllegalStateException("조회 오류 픽스처"))
            val source = FakeRamenShopRepository(shopDetailError = error)
            val result = DefaultShopMenuRepository(source).fetchShopMenu("shop")
            assertSame(error, (result as RamapResult.Error).error)
            assertEquals(listOf("shop"), source.requestedShopDetailIds)
        }
}
