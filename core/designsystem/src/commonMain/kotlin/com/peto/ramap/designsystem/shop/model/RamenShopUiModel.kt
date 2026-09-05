package com.peto.ramap.designsystem.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.designsystem.resource.information.ShopInformationFieldResourceMapper
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop

@Immutable
data class RamenShopUiModel(
    val shop: RamenShop,
    val waitingVisible: Boolean,
) {
    val reportFieldOptions: List<ReportFieldOption> =
        buildList {
            add(reportFieldOption(ShopInformationField.ADDRESS))
            if (shop.businessHoursDetails != null) {
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
            label = ShopInformationFieldResourceMapper.label(field),
        )
}
