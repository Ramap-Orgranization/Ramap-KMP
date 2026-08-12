package com.peto.ramap.designsystem.resource.category

import com.peto.ramap.domain.model.shop.Category
import org.jetbrains.compose.resources.StringResource
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.menu_category_aburasoba
import ramap.shared.generated.resources.menu_category_chanke
import ramap.shared.generated.resources.menu_category_hiyashi
import ramap.shared.generated.resources.menu_category_iekei
import ramap.shared.generated.resources.menu_category_jiro
import ramap.shared.generated.resources.menu_category_mazesoba
import ramap.shared.generated.resources.menu_category_miso
import ramap.shared.generated.resources.menu_category_niboshi_gyokai
import ramap.shared.generated.resources.menu_category_shio
import ramap.shared.generated.resources.menu_category_shoyu
import ramap.shared.generated.resources.menu_category_tomato
import ramap.shared.generated.resources.menu_category_tonkotsu
import ramap.shared.generated.resources.menu_category_tori
import ramap.shared.generated.resources.menu_category_tsukemen

object CategoryResourceMapper {
    fun label(category: Category): StringResource =
        when (category) {
            Category.TONKOTSU -> Res.string.menu_category_tonkotsu
            Category.SHOYU -> Res.string.menu_category_shoyu
            Category.SHIO -> Res.string.menu_category_shio
            Category.MISO -> Res.string.menu_category_miso
            Category.TORI -> Res.string.menu_category_tori
            Category.CHANKE -> Res.string.menu_category_chanke
            Category.TSUKEMEN -> Res.string.menu_category_tsukemen
            Category.MAZESOBA -> Res.string.menu_category_mazesoba
            Category.ABURASOBA -> Res.string.menu_category_aburasoba
            Category.JIRO -> Res.string.menu_category_jiro
            Category.NIBOSHI_GYOKAI -> Res.string.menu_category_niboshi_gyokai
            Category.IEKEI -> Res.string.menu_category_iekei
            Category.HIYASHI -> Res.string.menu_category_hiyashi
            Category.TOMATO -> Res.string.menu_category_tomato
        }
}
