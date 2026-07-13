package com.peto.ramap.ui.main.model

import com.peto.ramap.domain.model.ShopInformationField
import com.peto.ramap.fixture.ramenShopFixture
import com.peto.ramap.ui.main.map.model.RamenShopUiModel
import kotlin.test.Test
import kotlin.test.assertEquals

class RamenShopUiModelTest {
    @Test
    fun `신고 필드 옵션은 표시 가능한 매장 정보만 포함한다`() {
        val uiModel =
            RamenShopUiModel(
                shop =
                    ramenShopFixture(
                        kakaoPlaceUrl = null,
                        phone = null,
                        businessHours = null,
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
}
