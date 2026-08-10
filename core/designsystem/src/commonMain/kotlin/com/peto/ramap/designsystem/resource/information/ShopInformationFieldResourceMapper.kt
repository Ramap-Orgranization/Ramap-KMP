package com.peto.ramap.designsystem.resource.information

import com.peto.ramap.domain.model.report.ShopInformationField
import org.jetbrains.compose.resources.StringResource
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

object ShopInformationFieldResourceMapper {
    fun label(field: ShopInformationField): StringResource =
        when (field) {
            ShopInformationField.ADDRESS -> Res.string.shop_information_field_address
            ShopInformationField.PHONE -> Res.string.shop_information_field_phone
            ShopInformationField.BUSINESS_HOURS -> Res.string.shop_information_field_business_hours
            ShopInformationField.MENU_CATEGORIES -> Res.string.shop_information_field_menu_categories
            ShopInformationField.WAITING -> Res.string.shop_information_field_waiting
            ShopInformationField.INSTAGRAM -> Res.string.shop_information_field_instagram
            ShopInformationField.KAKAO_MAP -> Res.string.shop_information_field_kakao_map
            ShopInformationField.NAVER_MAP -> Res.string.shop_information_field_naver_map
            ShopInformationField.OTHER -> Res.string.shop_information_field_other
        }
}
