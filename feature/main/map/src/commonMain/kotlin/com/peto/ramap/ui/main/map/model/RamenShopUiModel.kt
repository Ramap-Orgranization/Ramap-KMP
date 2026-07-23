package com.peto.ramap.ui.main.map.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.resource.category.label
import com.peto.ramap.ui.resource.information.label

@Immutable
data class RamenShopUiModel(
    val shop: RamenShop,
    val waitingVisible: Boolean,
) {
    val reportFieldOptions: List<ReportFieldOption> =
        buildList {
            add(reportFieldOption(ShopInformationField.ADDRESS))
            if (shop.phone != null) {
                add(reportFieldOption(ShopInformationField.PHONE))
            }
            if (shop.businessHours != null) {
                add(reportFieldOption(ShopInformationField.BUSINESS_HOURS))
            }
            if (shop.hasCategory) {
                add(reportFieldOption(ShopInformationField.MENU_CATEGORIES))
            }
            if (waitingVisible) {
                add(reportFieldOption(ShopInformationField.WAITING))
            }
            if (shop.instagramUrl != null) {
                add(reportFieldOption(ShopInformationField.INSTAGRAM))
            }
            if (shop.kakaoPlaceUrl != null) {
                add(reportFieldOption(ShopInformationField.KAKAO_MAP))
            }
            if (shop.naverPlaceUrl != null) {
                add(reportFieldOption(ShopInformationField.NAVER_MAP))
            }
            add(reportFieldOption(ShopInformationField.OTHER))
        }

    private fun reportFieldOption(field: ShopInformationField) =
        ReportFieldOption(
            field = field,
            label = field.label(),
        )
}
