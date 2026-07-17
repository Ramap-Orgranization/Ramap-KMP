package com.peto.ramap.ui.main.map.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.shop_information_field_address
import ramap.shared.generated.resources.shop_information_field_business_hours
import ramap.shared.generated.resources.shop_information_field_instagram
import ramap.shared.generated.resources.shop_information_field_kakao_map
import ramap.shared.generated.resources.shop_information_field_menu_categories
import ramap.shared.generated.resources.shop_information_field_naver_map
import ramap.shared.generated.resources.shop_information_field_other
import ramap.shared.generated.resources.shop_information_field_phone
import ramap.shared.generated.resources.shop_information_field_waiting

@Immutable
data class RamenShopUiModel(
    val shop: RamenShop,
    val waitingVisible: Boolean,
) {
    val reportFieldOptions: List<ReportFieldOption>
        get() =
            buildList {
                add(ReportFieldOption(ShopInformationField.ADDRESS, Res.string.shop_information_field_address))
                if (shop.phone != null) {
                    add(ReportFieldOption(ShopInformationField.PHONE, Res.string.shop_information_field_phone))
                }
                if (shop.businessHours != null) {
                    add(ReportFieldOption(ShopInformationField.BUSINESS_HOURS, Res.string.shop_information_field_business_hours))
                }
                if (shop.hasCategory) {
                    add(ReportFieldOption(ShopInformationField.MENU_CATEGORIES, Res.string.shop_information_field_menu_categories))
                }
                if (waitingVisible) {
                    add(ReportFieldOption(ShopInformationField.WAITING, Res.string.shop_information_field_waiting))
                }
                if (shop.instagramUrl != null) {
                    add(ReportFieldOption(ShopInformationField.INSTAGRAM, Res.string.shop_information_field_instagram))
                }
                if (shop.kakaoPlaceUrl != null) {
                    add(ReportFieldOption(ShopInformationField.KAKAO_MAP, Res.string.shop_information_field_kakao_map))
                }
                if (shop.naverPlaceUrl != null) {
                    add(ReportFieldOption(ShopInformationField.NAVER_MAP, Res.string.shop_information_field_naver_map))
                }
                add(ReportFieldOption(ShopInformationField.OTHER, Res.string.shop_information_field_other))
            }
}
