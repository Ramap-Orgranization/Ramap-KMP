package com.peto.ramap.designsystem.shop

import com.peto.ramap.designsystem.shop.model.RamenShopUiModel
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.fixture.ramenShopFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RamenShopUiModelTest {
    @Test
    fun `신고 필드 옵션은 표시 가능한 매장 정보만 포함한다`() {
        val uiModel =
            RamenShopUiModel(
                shop =
                    ramenShopFixture(
                        kakaoPlaceUrl = null,
                        phone = null,
                        instagramUrl = null,
                        menuCategories = emptyList(),
                    ),
                waitingVisible = true,
            )

        assertEquals(
            listOf(
                ShopInformationField.ADDRESS,
                ShopInformationField.WAITING,
                ShopInformationField.OTHER,
            ),
            uiModel.reportFieldOptions.map { it.field },
        )
    }

    @Test
    fun `신고 필드 옵션은 UI 모델 인스턴스에서 한 번만 생성한다`() {
        val uiModel =
            RamenShopUiModel(
                shop = ramenShopFixture(),
                waitingVisible = true,
            )

        assertSame(uiModel.reportFieldOptions, uiModel.reportFieldOptions)
    }
}
