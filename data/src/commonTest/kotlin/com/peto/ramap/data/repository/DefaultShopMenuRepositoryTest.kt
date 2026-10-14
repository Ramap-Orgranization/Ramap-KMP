package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.core.result.getOrThrow
import com.peto.ramap.data.model.MenuResponse
import com.peto.ramap.data.model.MenuSectionResponse
import com.peto.ramap.data.model.ShopMenuResponse
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.fake.FakeShopMenuDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultShopMenuRepositoryTest {
    @Test
    fun `메뉴 전용 조회는 갱신 시각을 반환한다`() =
        runTest {
            val response =
                ShopMenuResponse(
                    menuUpdatedAt = "2026-09-02T12:00:00Z",
                    menuSections =
                        listOf(
                            MenuSectionResponse(
                                id = "section",
                                shopId = "menu-update-shop",
                                title = "상시메뉴",
                                displayOrder = 0,
                            ),
                        ),
                    menuItems =
                        listOf(
                            MenuResponse(
                                id = "menu",
                                sectionId = "section",
                                name = "쇼유라멘",
                                displayOrder = 0,
                            ),
                        ),
                )
            val repository =
                DefaultShopMenuRepository(
                    FakeShopMenuDataSource(response = response),
                )

            val detail = repository.fetchShopMenu("menu-update-shop").getOrThrow()

            assertEquals("2026-09-02T12:00:00Z", detail.updatedAt)
        }

    @Test
    fun `이벤트 메뉴 섹션은 상시 메뉴보다 먼저 표시한다`() =
        runTest {
            val response =
                ShopMenuResponse(
                    menuSections =
                        listOf(
                            MenuSectionResponse(
                                id = "permanent",
                                shopId = "menu-order-shop",
                                title = " 상시메뉴 ",
                                displayOrder = 0,
                            ),
                            MenuSectionResponse(
                                id = "event",
                                shopId = "menu-order-shop",
                                title = "기간 한정",
                                displayOrder = 1,
                            ),
                        ),
                    menuItems =
                        listOf(
                            MenuResponse(
                                id = "permanent-item",
                                sectionId = "permanent",
                                name = "쇼유라멘",
                                displayOrder = 0,
                            ),
                            MenuResponse(
                                id = "event-item",
                                sectionId = "event",
                                name = "한정 라멘",
                                displayOrder = 0,
                            ),
                        ),
                )
            val repository =
                DefaultShopMenuRepository(
                    FakeShopMenuDataSource(response = response),
                )

            val detail = repository.fetchShopMenu("menu-order-shop").getOrThrow()

            assertEquals(listOf("event", "permanent"), detail.sections.map { it.id })
        }

    @Test
    fun `메뉴가 없는 매장은 성공한 빈 메뉴를 반환한다`() =
        runTest {
            val source = FakeShopMenuDataSource()

            val result = DefaultShopMenuRepository(source).fetchShopMenu("shop").getOrThrow()

            assertEquals(ShopMenu(emptyList()), result)
            assertEquals(listOf("shop"), source.requestedShopIds)
        }

    @Test
    fun `메뉴 조회 실패를 성공한 빈 메뉴로 바꾸지 않는다`() =
        runTest {
            val error = IllegalStateException("조회 오류 픽스처")
            val source = FakeShopMenuDataSource(error = error)

            val result = DefaultShopMenuRepository(source).fetchShopMenu("shop")

            assertEquals(RamapResult.Error(RamapError.Unknown(error)), result)
            assertEquals(listOf("shop"), source.requestedShopIds)
        }
}
